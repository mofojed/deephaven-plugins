import React, { useCallback, useState } from 'react';
import {
  SpectrumNumberFieldProps,
  NumberField as SpectrumNumberField,
} from '@adobe/react-spectrum';
import { useDebouncedCallback } from '@deephaven/react-hooks';

const VALUE_CHANGE_DEBOUNCE = 250;

const EMPTY_FUNCTION = () => undefined;

function NumberField(props: SpectrumNumberFieldProps) {
  const {
    defaultValue = 0,
    value: propValue,
    onChange: propOnChange = EMPTY_FUNCTION,
    ...otherProps
  } = props;

  const [value, setValue] = useState(propValue ?? defaultValue);

  const debouncedOnChange = useDebouncedCallback(
    propOnChange,
    VALUE_CHANGE_DEBOUNCE
  );

  const onChange = useCallback(
    newValue => {
      setValue(newValue);
      debouncedOnChange(newValue);
    },
    [debouncedOnChange]
  );

  return (
    // eslint-disable-next-line react/jsx-props-no-spreading
    <SpectrumNumberField value={value} onChange={onChange} {...otherProps} />
  );
}

NumberField.displayName = 'TextField';

export default NumberField;
