import React, { useCallback } from 'react';
import { Form as SpectrumForm, SpectrumFormProps } from '@adobe/react-spectrum';

function Form(props: SpectrumFormProps & { onSubmit?: () => void }) {
  const { onSubmit: propOnSubmit, ...otherProps } = props;

  const onSubmit = useCallback(
    e => {
      // The PressEvent from React Spectrum is not serializable (contains circular references). We're just dropping the event here but we should probably convert it.
      // TODO(#76): Need to serialize PressEvent and send with the callback instead of just dropping it.
      propOnSubmit?.();
    },
    [propOnSubmit]
  );

  return (
    // eslint-disable-next-line react/jsx-props-no-spreading
    <SpectrumForm onSubmit={onSubmit} {...otherProps} />
  );
}

Form.displayName = 'Form';

export default Form;
