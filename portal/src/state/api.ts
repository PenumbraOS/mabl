import type { Conversation, ConversationWithMessages } from "./types";

type HTTPMethod = "GET" | "POST" | "PUT" | "DELETE";

const hostname = () => import.meta.env.VITE_HOSTNAME ?? "localhost:8080";

const queryFn = async (
  method: HTTPMethod = "GET",
  route: string,
  body?: string
) => {
  const url = `http://${hostname()}${route}`;
  const response = await fetch(url, {
    method,
    body,
  });

  if (!response.ok) {
    throw new Error("Network response was not ok");
  }
  return response.json();
};

export const getConversations = async (): Promise<Conversation[]> =>
  queryFn("GET", "/api/conversation");

export const getConversationById = async (
  id: string
): Promise<ConversationWithMessages> =>
  queryFn("GET", `/api/conversation/${id}`);
