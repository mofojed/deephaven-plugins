import React from 'react';
import { ScopedIDContext, useScopedID } from './useScopedId';

export type ScopedIDWrapperProps = {
  /** ID of the current scope */
  id: string;

  /** Children to render */
  children: React.ReactNode;
};

export function ScopedIDWrapper({ id, children }: ScopedIDWrapperProps) {
  const scopedId = useScopedID(id);
  return (
    <ScopedIDContext.Provider value={scopedId}>
      {children}
    </ScopedIDContext.Provider>
  );
}

export default ScopedIDWrapper;
