/**
 * Handles document events for one widget.
 */
import React, {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react';
import {
  JSONRPCClient,
  JSONRPCServer,
  JSONRPCServerAndClient,
} from 'json-rpc-2.0';
import { useApi } from '@deephaven/jsapi-bootstrap';
import type { Widget, WidgetExportedObject } from '@deephaven/jsapi-types';
import Log from '@deephaven/log';
import {
  CALLABLE_KEY,
  ElementNode,
  OBJECT_KEY,
  isCallableNode,
  isObjectNode,
} from './ElementUtils';
import { WidgetMessageEvent, WidgetWrapper } from './WidgetTypes';
import DocumentHandler from './DocumentHandler';

const log = Log.module('@deephaven/js-plugin-ui/WidgetHandler');

export interface WidgetHandlerProps {
  /** Widget for this to handle */
  widget: WidgetWrapper;

  /** Triggered when all panels opened from this widget have closed */
  onClose?: (widgetId: string) => void;
}

function WidgetHandler({ onClose, widget: wrapper }: WidgetHandlerProps) {
  const dh = useApi();

  const [widget, setWidget] = useState<Widget>();
  const [element, setElement] = useState<ElementNode>();

  // When we fetch a widget, the client is then responsible for the exported objects.
  // These objects could stay alive even after the widget is closed if we wanted to,
  // but for our use case we want to close them when the widget is closed, so we close them all on unmount.
  const exportedObjectMap = useRef<Map<number, WidgetExportedObject>>(
    new Map()
  );
  const exportedObjectCount = useRef(0);

  // Bi-directional communication as defined in https://www.npmjs.com/package/json-rpc-2.0
  const jsonClient = useMemo(
    () =>
      widget != null
        ? new JSONRPCServerAndClient(
            new JSONRPCServer(),
            new JSONRPCClient(request => {
              log.debug('Sending request', request);
              widget.sendMessage(JSON.stringify(request), []);
            })
          )
        : null,
    [widget]
  );
  const parseDocument = useCallback(
    /**
     * Parse the data from the server, replacing any callable nodes with functions that call the server.
     * Replaces all Callables with an async callback that will automatically call the server use JSON-RPC.
     * Replaces all Objects with the exported object from the server.
     * Element nodes are not replaced. Those are handled in `DocumentHandler`.
     *
     * @param data The data to parse
     * @returns The parsed data
     */
    (data: string) => {
      // Keep track of exported objects that are no longer in use after this render.
      // We close those objects that are no longer referenced, as they will never be referenced again.
      const deadObjectMap = new Map(exportedObjectMap.current);

      const parsedData = JSON.parse(data, (key, value) => {
        // Need to re-hydrate any objects that are defined
        if (isCallableNode(value)) {
          const callableId = value[CALLABLE_KEY];
          log.debug2('Registering callableId', callableId);
          return async (...args: unknown[]) => {
            log.debug('Callable called', callableId, ...args);
            return jsonClient?.request(callableId, args);
          };
        }
        if (isObjectNode(value)) {
          // Replace this node with the exported object
          const objectKey = value[OBJECT_KEY];
          const exportedObject = exportedObjectMap.current.get(objectKey);
          if (exportedObject === undefined) {
            // The map should always have the exported object for a key, otherwise the protocol is broken
            throw new Error(`Invalid exported object key ${objectKey}`);
          }
          deadObjectMap.delete(objectKey);
          return exportedObject;
        }

        return value;
      });

      // Close any objects that are no longer referenced
      deadObjectMap.forEach((deadObject, objectKey) => {
        log.debug('Closing dead object', objectKey);
        deadObject.close();
        exportedObjectMap.current.delete(objectKey);
      });

      log.debug2(
        'Parsed data',
        parsedData,
        'exportedObjectMap',
        exportedObjectMap.current,
        'deadObjectMap',
        deadObjectMap
      );
      return parsedData;
    },
    [jsonClient]
  );

  const updateExportedObjects = useCallback(
    (newExportedObjects: WidgetExportedObject[]) => {
      for (let i = 0; i < newExportedObjects.length; i += 1) {
        const exportedObject = newExportedObjects[i];
        const exportedObjectKey = exportedObjectCount.current;
        exportedObjectCount.current += 1;
        exportedObjectMap.current.set(exportedObjectKey, exportedObject);
      }
    },
    []
  );

  useEffect(
    function initMethods() {
      if (jsonClient == null) {
        return;
      }

      log.debug('Adding methods to jsonClient');
      jsonClient.addMethod('documentUpdated', async (params: [string]) => {
        log.debug2('documentUpdated', params[0]);
        const newDocument = parseDocument(params[0]);
        setElement(newDocument);
      });

      return () => {
        jsonClient.rejectAllPendingRequests('Widget was changed');
      };
    },
    [jsonClient, parseDocument]
  );

  useEffect(() => {
    if (widget == null) {
      return;
    }
    function receiveData(
      data: string,
      newExportedObjects: WidgetExportedObject[]
    ) {
      log.debug2('Data received', data, newExportedObjects);
      updateExportedObjects(newExportedObjects);
      jsonClient?.receiveAndSend(JSON.parse(data));
    }

    const cleanup = widget.addEventListener(
      dh.Widget.EVENT_MESSAGE,
      (event: WidgetMessageEvent) => {
        receiveData(
          event.detail.getDataAsString(),
          event.detail.exportedObjects
        );
      }
    );

    log.debug('Receiving initial data');
    // We need to get the initial data and process it. It should be a documentUpdated command.
    receiveData(widget.getDataAsString(), widget.exportedObjects);

    return () => {
      log.debug('Cleaning up listener');
      cleanup();
    };
  }, [dh, jsonClient, parseDocument, updateExportedObjects, widget]);

  useEffect(
    function loadWidget() {
      log.debug('loadWidget', wrapper.id, wrapper.definition);
      let isCancelled = false;
      async function loadWidgetInternal() {
        const newWidget = await wrapper.fetch();
        if (isCancelled) {
          newWidget.close();
          return;
        }
        log.debug('newWidget', wrapper.id, wrapper.definition, newWidget);
        setWidget(newWidget);
      }
      loadWidgetInternal();
      return () => {
        isCancelled = true;
      };
    },
    [wrapper]
  );

  useEffect(
    () =>
      function closeWidget() {
        widget?.close();
      },
    [widget]
  );

  const handleDocumentClose = useCallback(() => {
    log.debug('Widget document closed', wrapper.id);
    onClose?.(wrapper.id);
  }, [onClose, wrapper.id]);

  return useMemo(
    () =>
      element ? (
        <DocumentHandler
          definition={wrapper.definition}
          element={element}
          onClose={handleDocumentClose}
        />
      ) : null,
    [element, handleDocumentClose, wrapper]
  );
}

WidgetHandler.displayName = '@deephaven/js-plugin-ui/WidgetHandler';

export default WidgetHandler;
