import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { Conversation } from "../components/Conversation";

export const Route = createFileRoute("/conversation/$conversationId")({
  component: ConversationPage,
});

function ConversationPage() {
  const { conversationId } = Route.useParams();
  const navigate = useNavigate();

  const handleBack = () => {
    navigate({ to: "/" });
  };

  return <Conversation id={conversationId} onBack={handleBack} />;
}
