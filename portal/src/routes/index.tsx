import { createFileRoute } from "@tanstack/react-router";
import { Conversations } from "../components/Conversations";

export const Route = createFileRoute("/")({
  component: ConversationsPage,
});

function ConversationsPage() {
  return <Conversations />;
}
