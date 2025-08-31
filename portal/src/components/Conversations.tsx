import { useQuery } from "@tanstack/react-query";
import * as React from "react";
import { getConversations } from "../state/api";
import { QueryWrapper } from "./QueryWrapper";
import type { Conversation } from "../state/types";
import { Button } from "@mantine/core";

export const Conversations: React.FC<{
  setActiveConversationId: (id: string) => void;
}> = ({ setActiveConversationId }) => {
  const result = useQuery({
    queryKey: ["conversations"],
    queryFn: getConversations,
  });

  return (
    <QueryWrapper
      result={result}
      DataComponent={ConversationData}
      setActiveConversationId={setActiveConversationId}
    />
  );
};

const ConversationData: React.FC<{
  data: Conversation[];
  setActiveConversationId: (id: string) => void;
}> = ({ data, setActiveConversationId }) => {
  return (
    <div>
      {data.map((conversation) => {
        return (
          <div>
            <div>{conversation.title}</div>
            <div>Last active: {conversation.lastActivity}</div>
            <Button onClick={() => setActiveConversationId(conversation.id)}>
              View
            </Button>
          </div>
        );
      })}
    </div>
  );
};
