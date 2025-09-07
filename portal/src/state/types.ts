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
  images?: ConversationImage[];
}

export interface ConversationImage {
  id: number;
  messageId: number;
  fileName: string;
  mimeType: string;
  fileSizeBytes: number;
  width?: number;
  height?: number;
  timestamp: number;
}

export type ConversationWithMessages = Conversation & {
  messages: ConversationMessage[];
};
