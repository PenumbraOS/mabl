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
      {!!activeConversationId ? (
        <Conversation 
          id={activeConversationId} 
          onBack={() => setActiveConversationId(undefined)}
        />
      ) : (
        <Conversations setActiveConversationId={setActiveConversationId} />
      )}
    </div>
  );
};
