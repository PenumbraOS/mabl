export interface Conversation {
  id: string;
  title: string;
  createdAt: number;
  lastActivity: number;
  isActive: boolean;
}

export interface ConversationMessage {
  conversationId: string;
  type: "user" | "assistant" | "tool";
  content: string;
  // TODO: Deserialize JSON
  toolCalls: unknown;
  toolCallsId: string;
  timestamp: number;
}

export type ConversationWithMessages = Conversation & {
  messages: ConversationMessage[];
};
