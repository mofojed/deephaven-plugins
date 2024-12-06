import React, { useEffect, useMemo, useState } from 'react';
import classNames from 'classnames';
import {
  Editor as DHCEditor,
  EditorProps as DHCEditorProps,
} from '@deephaven/console';
import { EMPTY_FUNCTION } from '@deephaven/utils';
import useDebouncedOnChange from './hooks/useDebouncedOnChange';

export type EditorProps = {
  /** Class for the editor */
  className?: string;

  /** Default value for the editor (uncontrolled mode) */
  defaultValue?: string;

  /** Value for the editor (controlled mode) */
  value?: string;

  /** Language for the editor */
  language?: string;

  /** Callback for when the editors' value has changed */
  onChange?: ((value: string) => Promise<void>) | (() => void);
};

export function Editor({
  className,
  defaultValue = '',
  value: propValue,
  onChange: propOnChange = EMPTY_FUNCTION,
  language,
}: EditorProps): JSX.Element {
  const [editor, setEditor] =
    useState<Parameters<DHCEditorProps['onEditorInitialized']>[0]>();

  const [value, onChange] = useDebouncedOnChange(
    propValue ?? defaultValue,
    propOnChange
  );
  const settings = useMemo(() => ({ language, value }), [language, value]);

  useEffect(() => {
    if (!editor) {
      return;
    }

    editor.onDidChangeModelContent(e => {
      onChange(editor.getValue());
    });

    return () => {
      editor.dispose();
    };
  }, [editor, onChange]);

  return (
    <DHCEditor
      settings={settings}
      className={classNames('ui-editor', className)}
      onEditorInitialized={newEditor => setEditor(newEditor)}
    />
  );
}

export default Editor;
