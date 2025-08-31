import { useState } from "react";
import { Conversations } from "./components/Conversations";
import { Conversation } from "./components/Conversation";

export const App = () => {
  // TODO: Move to router
  const [activeConversationId, setActiveConversationId] = useState<
    string | undefined
  >(undefined);

  return (
    <div>
      <div>Hello world</div>
      {!!activeConversationId ? (
        <Conversation id={activeConversationId} />
      ) : (
        <Conversations setActiveConversationId={setActiveConversationId} />
      )}
    </div>
  );
};
