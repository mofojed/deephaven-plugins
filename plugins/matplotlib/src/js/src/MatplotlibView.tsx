import { CSSProperties, useEffect, useState } from 'react';
// import { useApi } from '@deephaven/jsapi-bootstrap';
import type { dh } from '@deephaven/jsapi-types';
import Log from '@deephaven/log';
import { WidgetComponentProps } from '@deephaven/plugin';

const log = Log.module('@deephaven/js-plugin-matplotlib.MatplotlibView');

// enum InputColumn {
//   key = 'key',
//   value = 'value',
// }

// enum InputKey {
//   revision = 'revision',
// }

export const MatplotlibViewStyle: CSSProperties = {
  height: '100%',
  width: '100%',
  display: 'contents',
};

export const MatplotlibViewImageStyle: CSSProperties = {
  height: '100%',
  width: '100%',
  objectFit: 'contain',
};

/**
 * Displays a rendered matplotlib from the server
 */
export function MatplotlibView(
  props: WidgetComponentProps<dh.Widget>
): JSX.Element {
  const { fetch } = props;
  const [imageSrc, setImageSrc] = useState<string>();
  // const [inputTable, setInputTable] = useState<dh.Table>();
  // Set revision to 0 until we're listening to the revision table
  // const [revision, setRevision] = useState<number>(0);
  // const dh = useApi();

  // useEffect(
  //   function initInputTable() {
  //     if (inputTable == null) {
  //       return;
  //     }

  //     const table = inputTable;
  //     async function openTable() {
  //       log.debug('openTable');
  //       const keyColumn = table.findColumn(InputColumn.key);
  //       const valueColumn = table.findColumn(InputColumn.value);
  //       // Filter on the 'revision' key, listen for updates on the value
  //       table.applyFilter([
  //         keyColumn.filter().eq(dh.FilterValue.ofString(InputKey.revision)),
  //       ]);
  //       table.addEventListener(
  //         dh.Table.EVENT_UPDATED,
  //         ({ detail: data }: CustomEvent<dh.ViewportData>) => {
  //           const newRevision = data.rows[0].get(valueColumn);
  //           log.debug('New revision', newRevision);
  //           setRevision(newRevision);
  //         }
  //       );
  //       table.setViewport(0, 0, [valueColumn]);
  //     }
  //     openTable();
  //     return function closeTable() {
  //       log.debug('closeTable');
  //       table.close();
  //     };
  //   },
  //   [dh, inputTable]
  // );

  useEffect(
    function updateData() {
      async function fetchData() {
        log.debug('fetchData');
        const widget = await fetch();
        const imageData = widget.getDataAsBase64();
        console.log('Data', imageData);
        setImageSrc(`data:image/png;base64,${imageData}`);
        widget.addEventListener('message', event => {
          log.debug('Message event', event);
          const newImageData = event.detail.getDataAsBase64();
          console.log('Message event', event, event.detail.exportedObjects);
          setImageSrc(`data:image/png;base64,${newImageData}`);
        });
      }
      fetchData();
    },
    [fetch]
  );

  return (
    <div className="matplotlib-view" style={MatplotlibViewStyle}>
      {imageSrc !== undefined && (
        <img
          src={imageSrc}
          alt="Matplotlib render"
          style={MatplotlibViewImageStyle}
        />
      )}
    </div>
  );
}

export default MatplotlibView;
