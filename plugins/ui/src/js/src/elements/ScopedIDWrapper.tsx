import React from 'react';
import { ScopedIdContext, useScopedId } from './useScopedId';

export type ScopedIDWrapperProps = {
  /** ID of the current scope */
  id: string;

  /** Delimiter to use when creating a new scoped ID */
  delimiter?: string;

  /** If true, create a new scoped ID */
  newScope?: boolean;

  /** Children to render */
  children: React.ReactNode;
};

/**
 * Wrapper that provides a scoped ID to its children, by adding to any parent scoped ID
 */
export function ScopedIDWrapper({
  id,
  children,
  newScope,
  delimiter,
}: ScopedIDWrapperProps) {
  const scopedId = useScopedId(id, newScope, delimiter);
  return (
    <ScopedIdContext.Provider value={scopedId}>
      {children}
    </ScopedIdContext.Provider>
  );
}

export default ScopedIDWrapper;
