import React, { useContext } from 'react';

export const ScopedIDContext = React.createContext<string>('');

/**
 * Use an ID that includes all of the parent IDs
 * @param id ID of this scope
 * @param scopeDelimiter Character to use to delimit the scopes when joining them together
 * @returns An ID that is all the parent scopes joined with the scopeDelimiter and the ID of this scope
 */
export function useScopedID(id: string, scopeDelimiter = '/'): string {
  const parentID = useContext(ScopedIDContext);
  return parentID.length > 0 ? `${parentID}${scopeDelimiter}${id}` : id;
}
