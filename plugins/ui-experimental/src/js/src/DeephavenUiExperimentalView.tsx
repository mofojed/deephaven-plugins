import React, { CSSProperties, useCallback, useEffect, useState } from "react";
import { useApi } from "@deephaven/jsapi-bootstrap";
import Log from "@deephaven/log";
import { WidgetComponentProps } from "@deephaven/plugin";
import type { Widget } from "@deephaven/jsapi-types";
import { DateTimeInput } from "@deephaven/components";

const log = Log.module("deephaven-ui-experimental.DeephavenUiExperimentalView");

export function DeephavenUiExperimentalView(
  props: WidgetComponentProps
): JSX.Element {
  const { fetch } = props;
  // TODO: Get the default date from the
  const [date, setDate] = useState<string>("2024-05-01 12:00:00.000000000");
  const [widget, setWidget] = useState<Widget | null>(null);
  const dh = useApi();

  useEffect(() => {
    async function init() {
      // Fetch the widget from the server
      const newWidget = (await fetch()) as Widget;
      setWidget(newWidget);

      // Get the initial state
      const newDate = newWidget.getDataAsString();
      setDate(newDate);

      // Add an event listener to the widget to listen for messages from the server
      newWidget.addEventListener<Widget>(
        dh.Widget.EVENT_MESSAGE,
        ({ detail }) => {
          const newDate = detail.getDataAsString();
          setDate(newDate);
        }
      );
    }

    init();
  }, [dh, fetch]);

  const handleChange = useCallback(
    (newDate?: string) => {
      log.info("handleChange", newDate, widget);
      if (newDate == null) {
        return;
      }

      const { timeZone } = Intl.DateTimeFormat().resolvedOptions();
      const dateLiteral = `${newDate.replace(" ", "T")} ${timeZone}`;
      widget?.sendMessage(dateLiteral);
    },
    [widget]
  );

  return <DateTimeInput onChange={handleChange} defaultValue={date} />;
}

export default DeephavenUiExperimentalView;
