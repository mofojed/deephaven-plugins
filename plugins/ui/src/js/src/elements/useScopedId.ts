import React, { useContext } from 'react';

export const SCOPE_ID_DELIMITER = '/';

export const ScopedIdContext = React.createContext<string>('');

/**
 * Use an ID that includes all of the parent IDs
 * @param id ID of this scope
 * @param newScope If true, this scope will be a new scope and not include the parent scope
 * @param scopeDelimiter Character to use to delimit the scopes when joining them together
 * @returns An ID that is all the parent scopes joined with the scopeDelimiter and the ID of this scope
 */
export function useScopedId(
  id: string,
  newScope = false,
  scopeDelimiter = SCOPE_ID_DELIMITER
): string {
  const parentID = useContext(ScopedIdContext);
  return !newScope && parentID.length > 0
    ? `${parentID}${scopeDelimiter}${id}`
    : id;
}
