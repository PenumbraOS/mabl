import { useQuery } from "@tanstack/react-query";
import * as React from "react";
import { useCallback } from "react";
import { getCameraRollImages, getCameraRollImageUrl } from "../state/api";
import { QueryWrapper } from "./QueryWrapper";
import { ImageGallery } from "./ImageGallery";
import { useImageGallery } from "../hooks/useImageGallery";
import type { CameraRollImage } from "../state/types";
import type { GalleryImage } from "./ImageGallery";
import {
  Card,
  Text,
  Group,
  Stack,
  Image,
  SimpleGrid,
  Badge,
} from "@mantine/core";
import layoutStyles from "../styles/layout.module.scss";
import styles from "./CameraRoll.module.scss";
import { formatHumanDate } from "../util/date";

export const CameraRoll: React.FC = () => {
  const result = useQuery({
    queryKey: ["cameraRoll"],
    queryFn: () => getCameraRollImages(100), // Get 100 images
  });

  const gallery = useImageGallery();

  return (
    <>
      <QueryWrapper
        result={result}
        DataComponent={CameraRollData}
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

const CameraRollData: React.FC<{
  data: CameraRollImage[];
  gallery: ReturnType<typeof useImageGallery>;
}> = ({ data, gallery }) => {
  const handleImageClick = useCallback(
    (index: number) => {
      const galleryImages: GalleryImage[] = data.map((image) => ({
        id: image.id,
        src: getCameraRollImageUrl(image.id),
        alt: image.fileName,
        title: image.fileName,
        metadata: `${formatHumanDate(image.dateTaken)} • ${image.width}×${image.height} • ${Math.round(image.size / 1024)} KB`,
      }));

      gallery.openGallery(galleryImages, index);
    },
    [data, gallery]
  );
  return (
    <Stack gap="md" p="md" className={layoutStyles.pageContainer}>
      <Group justify="space-between" align="center">
        <Text size="xl" fw={700}>
          Camera Roll
        </Text>
        <Badge variant="light" color="blue">
          {data.length} images
        </Badge>
      </Group>

      {data.length > 0 ? (
        <SimpleGrid cols={{ base: 2, sm: 3, md: 4, lg: 5 }} spacing="md">
          {data.map((image, index) => (
            <Card
              key={image.id}
              shadow="sm"
              padding="xs"
              radius="md"
              withBorder
              className={styles.imageCard}
              onClick={() => handleImageClick(index)}
            >
              <Card.Section>
                <Image
                  src={getCameraRollImageUrl(image.id)}
                  alt={image.fileName}
                  fit="cover"
                  h={200}
                  fallbackSrc="data:image/svg+xml,%3csvg%20width='200'%20height='200'%20xmlns='http://www.w3.org/2000/svg'%3e%3crect%20width='200'%20height='200'%20fill='%23f8f9fa'/%3e%3ctext%20x='100'%20y='100'%20font-family='Arial'%20font-size='12'%20fill='%23868e96'%20text-anchor='middle'%20dominant-baseline='middle'%3eImage%3c/text%3e%3c/svg%3e"
                />
              </Card.Section>

              <Stack gap="xs" mt="xs">
                <Text size="sm" fw={500} truncate>
                  {image.fileName}
                </Text>
                <Group justify="space-between">
                  <Text size="xs" c="dimmed">
                    {formatHumanDate(image.dateTaken)}
                  </Text>
                  <Text size="xs" c="dimmed">
                    {image.width}×{image.height}
                  </Text>
                </Group>
                <Text size="xs" c="dimmed">
                  {Math.round(image.size / 1024)} KB
                </Text>
              </Stack>
            </Card>
          ))}
        </SimpleGrid>
      ) : (
        <Text c="dimmed" ta="center" mt="xl">
          No images found in camera roll
        </Text>
      )}
    </Stack>
  );
};
