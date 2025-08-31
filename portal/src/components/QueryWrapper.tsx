import * as React from "react";

import { Alert, Loader } from "@mantine/core";
import type { UseQueryResult } from "@tanstack/react-query";
import { IconAlertTriangle } from "@tabler/icons-react";

type ExtractData<T> = T extends { data: unknown } ? T["data"] : never;

type Props<T> = {
  result: UseQueryResult<ExtractData<T>, Error>;
  DataComponent: React.FC<T>;
} & Omit<T, "data">;

export const QueryWrapper = <T extends { data: unknown }>({
  result,
  DataComponent,
  ...props
}: Props<T>) => {
  const { isPending, isError, data, error } = result;

  if (isPending) {
    return <Loader color="blue" />;
  }

  if (isError) {
    return (
      <Alert
        color="red"
        icon={<IconAlertTriangle />}
        title={`Error: ${error.name}`}
      >
        {error.message}
      </Alert>
    );
  }

  return <DataComponent {...({ ...props, data } as unknown as T)} />;
};
