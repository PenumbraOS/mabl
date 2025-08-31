import { useQuery } from "@tanstack/react-query";
import * as React from "react";
import { getConversationById } from "../state/api";
import { QueryWrapper } from "./QueryWrapper";
import type {
  ConversationMessage,
  ConversationWithMessages,
} from "../state/types";

export const Conversation: React.FC<{
  id: string;
}> = ({ id }) => {
  const result = useQuery({
    queryKey: ["conversation", id],
    queryFn: ({ queryKey }) => getConversationById(queryKey[1]),
  });

  return <QueryWrapper result={result} DataComponent={ConversationData} />;
};

const ConversationData: React.FC<{
  data: ConversationWithMessages;
}> = ({ data }) => {
  return (
    <div>
      <h3>{data.title}</h3>
      <h4>Messages</h4>
      <div>
        {data.messages.map((message) => {
          return (
            <div>
              {displayType(message.type)}
              <br />
              {message.content}
            </div>
          );
        })}
      </div>
    </div>
  );
};

const displayType = (type: ConversationMessage["type"]) => {
  switch (type) {
    case "assistant":
      return "MABL";
    case "user":
      return "User";
    case "tool":
      return "Tool";
  }
};
