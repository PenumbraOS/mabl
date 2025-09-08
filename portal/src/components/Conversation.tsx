import { useQuery } from "@tanstack/react-query";
import * as React from "react";
import { useCallback } from "react";
import { getConversationById, getImageUrl } from "../state/api";
import { QueryWrapper } from "./QueryWrapper";
import type {
  ConversationMessage,
  ConversationWithMessages,
  ConversationImage,
} from "../state/types";
import {
  Stack,
  Text,
  Card,
  Badge,
  Image,
  Button,
  Group,
  Box,
} from "@mantine/core";
import { ImageGallery } from "./ImageGallery";
import { useImageGallery } from "../hooks/useImageGallery";
import type { GalleryImage } from "./ImageGallery";
import layoutStyles from "../styles/layout.module.scss";
import styles from "./Conversation.module.scss";

export const Conversation: React.FC<{
  id: string;
  onBack?: () => void;
}> = ({ id, onBack }) => {
  const result = useQuery({
    queryKey: ["conversation", id],
    queryFn: ({ queryKey }) => getConversationById(queryKey[1]),
  });

  const gallery = useImageGallery();

  return (
    <>
      <QueryWrapper
        result={result}
        DataComponent={ConversationData}
        onBack={onBack}
        gallery={gallery}
      />
      <ImageGallery
        images={gallery.images}
        initialIndex={gallery.initialIndex}
        opened={gallery.opened}
        onClose={gallery.closeGallery}
      />
    </>
  );
};

const formatMessageDate = (timestamp: number) => {
  const date = new Date(timestamp);
  return date.toLocaleString();
};

const MessageImages: React.FC<{
  images: ConversationImage[] | undefined;
  onImageClick: (images: ConversationImage[], index: number) => void;
}> = ({ images, onImageClick }) => {
  if (!images || images.length === 0) {
    return null;
  }

  return (
    <Group gap="sm" mt="xs">
      {images.map((image, index) => (
        <Box key={image.id} className={styles.messageImageContainer}>
          <Image
            src={getImageUrl(image.fileName)}
            alt={`Image: ${image.fileName}`}
            fit="contain"
            radius="md"
            className={styles.messageImage}
            onClick={() => onImageClick(images, index)}
            fallbackSrc="data:image/svg+xml,%3csvg%20width='100'%20height='100'%20xmlns='http://www.w3.org/2000/svg'%3e%3crect%20width='100'%20height='100'%20fill='%23f8f9fa'/%3e%3ctext%20x='50'%20y='50'%20font-family='Arial'%20font-size='12'%20fill='%23868e96'%20text-anchor='middle'%20dominant-baseline='middle'%3eImage%3c/text%3e%3c/svg%3e"
          />
          <Text size="xs" c="dimmed" mt={4}>
            {image.fileName} ({Math.round(image.fileSizeBytes / 1024)} KB)
            {image.width && image.height && ` • ${image.width}×${image.height}`}
          </Text>
        </Box>
      ))}
    </Group>
  );
};

const ConversationData: React.FC<{
  data: ConversationWithMessages;
  onBack?: () => void;
  gallery: ReturnType<typeof useImageGallery>;
}> = ({ data, onBack, gallery }) => {
  const handleImageClick = useCallback(
    (images: ConversationImage[], index: number) => {
      const galleryImages: GalleryImage[] = images.map((image) => ({
        id: image.id,
        src: getImageUrl(image.fileName),
        alt: image.fileName,
        title: image.fileName,
        metadata: `${Math.round(image.fileSizeBytes / 1024)} KB${image.width && image.height ? ` • ${image.width}×${image.height}` : ""}`,
      }));

      gallery.openGallery(galleryImages, index);
    },
    [gallery]
  );
  return (
    <Stack gap="md" p="md" className={layoutStyles.pageContainer}>
      <Group justify="space-between" align="center">
        <Text size="xl" fw={700} truncate className={styles.conversationTitle}>
          {data.title}
        </Text>
        {onBack && (
          <Button variant="light" onClick={onBack}>
            Back to Conversations
          </Button>
        )}
      </Group>

      <Stack gap="md">
        {data.messages.length > 0 ? (
          data.messages.map((message, index) => (
            <Card
              key={`${message.conversationId}-${index}`}
              shadow="sm"
              padding="md"
              radius="md"
              withBorder
            >
              <Group justify="space-between" align="flex-start" mb="xs">
                <Badge
                  color={
                    message.type === "user"
                      ? "blue"
                      : message.type === "assistant"
                        ? "green"
                        : "gray"
                  }
                  variant="light"
                >
                  {displayType(message.type)}
                </Badge>
                <Text size="xs" c="dimmed">
                  {formatMessageDate(message.timestamp)}
                </Text>
              </Group>

              {message.content && (
                <Box>
                  <Text
                    style={{ whiteSpace: "pre-wrap", wordBreak: "break-word" }}
                  >
                    {message.content}
                  </Text>
                </Box>
              )}

              <MessageImages
                images={message.images}
                onImageClick={handleImageClick}
              />

              {/* {message.toolCalls && (
              <Paper withBorder p="xs" mt="sm" bg="gray.0">
                <Text size="xs" c="dimmed" mb="xs">
                  Tool calls:
                </Text>
                <Text size="xs" ff="monospace">
                  {JSON.stringify(message.toolCalls, null, 2)}
                </Text>
              </Paper>
            )} */}
            </Card>
          ))
        ) : (
          <Text c="dimmed" ta="center" mt="xl">
            No messages in this conversation yet
          </Text>
        )}
      </Stack>
    </Stack>
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
