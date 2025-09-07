import { useQuery } from "@tanstack/react-query";
import * as React from "react";
import { getConversations } from "../state/api";
import { QueryWrapper } from "./QueryWrapper";
import type { Conversation } from "../state/types";
import { Card, Text, Group, Button, Stack, Badge } from "@mantine/core";
import { Link } from "@tanstack/react-router";
import styles from "../styles/layout.module.scss";
import { formatHumanDate } from "../util/date";

export const Conversations: React.FC = () => {
  const result = useQuery({
    queryKey: ["conversations"],
    queryFn: getConversations,
  });

  return <QueryWrapper result={result} DataComponent={ConversationData} />;
};

const ConversationData: React.FC<{
  data: Conversation[];
}> = ({ data }) => {
  return (
    <Stack gap="md" p="md" className={styles.pageContainer}>
      <Group justify="space-between" align="center">
        <Text size="xl" fw={700}>
          Conversations
        </Text>
      </Group>
      {data.length > 0 ? (
        data.map((conversation) => (
          <Link
            key={conversation.id}
            to="/conversation/$conversationId"
            params={{ conversationId: conversation.id }}
            style={{ textDecoration: "none", color: "inherit" }}
          >
            <Card
              shadow="sm"
              padding="lg"
              radius="md"
              withBorder
              style={{ cursor: "pointer" }}
            >
              <Group justify="space-between" align="flex-start">
                <div style={{ flex: 1 }}>
                  <Group gap="xs" mb="xs">
                    <Text fw={500} truncate style={{ maxWidth: "400px" }}>
                      {conversation.title}
                    </Text>
                    {conversation.isActive && (
                      <Badge color="green" size="xs">
                        Active
                      </Badge>
                    )}
                  </Group>
                  <Text
                    size="sm"
                    c="dimmed"
                    title={new Date(conversation.lastActivity).toLocaleString()}
                  >
                    {formatHumanDate(conversation.lastActivity)}
                  </Text>
                </div>
                <Button variant="light" size="sm">
                  View
                </Button>
              </Group>
            </Card>
          </Link>
        ))
      ) : (
        <Text c="dimmed" ta="center" mt="xl">
          No conversations yet
        </Text>
      )}
    </Stack>
  );
};
