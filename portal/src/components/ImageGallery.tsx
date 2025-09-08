import React, { useState, useEffect, useCallback } from "react";
import { Modal, Image, Text, ActionIcon, Transition } from "@mantine/core";
import { IconChevronLeft, IconChevronRight, IconX } from "@tabler/icons-react";
import styles from "./ImageGallery.module.scss";

export interface GalleryImage {
  id: string | number;
  src: string;
  alt?: string;
  title?: string;
  metadata?: string;
}

interface ImageGalleryProps {
  images: GalleryImage[];
  initialIndex: number;
  opened: boolean;
  onClose: () => void;
}

export const ImageGallery: React.FC<ImageGalleryProps> = ({
  images,
  initialIndex,
  opened,
  onClose,
}) => {
  const [currentIndex, setCurrentIndex] = useState(initialIndex);
  const [imageVisible, setImageVisible] = useState(true);

  useEffect(() => {
    setCurrentIndex(initialIndex);
  }, [initialIndex]);

  const goToPrevious = useCallback(() => {
    if (images.length <= 1) return;
    setImageVisible(false);
    setTimeout(() => {
      setCurrentIndex((prev) => (prev > 0 ? prev - 1 : images.length - 1));
      setImageVisible(true);
    }, 150);
  }, [images.length]);

  const goToNext = useCallback(() => {
    if (images.length <= 1) return;
    setImageVisible(false);
    setTimeout(() => {
      setCurrentIndex((prev) => (prev < images.length - 1 ? prev + 1 : 0));
      setImageVisible(true);
    }, 150);
  }, [images.length]);

  const handleKeyDown = useCallback(
    (event: KeyboardEvent) => {
      if (!opened) return;

      switch (event.key) {
        case "ArrowLeft":
          event.preventDefault();
          goToPrevious();
          break;
        case "ArrowRight":
          event.preventDefault();
          goToNext();
          break;
        case "Escape":
          event.preventDefault();
          onClose();
          break;
      }
    },
    [opened, goToPrevious, goToNext, onClose]
  );

  useEffect(() => {
    document.addEventListener("keydown", handleKeyDown);
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, [handleKeyDown]);

  if (images.length === 0) return null;

  const currentImage = images[currentIndex];

  return (
    <Modal
      opened={opened}
      onClose={onClose}
      size="90vw"
      centered
      withCloseButton={false}
      overlayProps={{ backgroundOpacity: 0.9, blur: 3 }}
      transitionProps={{ transition: "fade", duration: 200 }}
      styles={{
        content: { backgroundColor: "transparent" },
        body: { padding: 0 },
      }}
    >
      <div className={styles.galleryContainer}>
        {/* Close button */}
        <ActionIcon
          variant="filled"
          color="dark"
          size="lg"
          className={styles.closeButton}
          onClick={onClose}
        >
          <IconX size={20} />
        </ActionIcon>

        {/* Navigation buttons */}
        {images.length > 1 && (
          <>
            <ActionIcon
              variant="filled"
              color="dark"
              size="xl"
              className={`${styles.navButton} ${styles.navLeft}`}
              onClick={goToPrevious}
            >
              <IconChevronLeft size={24} />
            </ActionIcon>

            <ActionIcon
              variant="filled"
              color="dark"
              size="xl"
              className={`${styles.navButton} ${styles.navRight}`}
              onClick={goToNext}
            >
              <IconChevronRight size={24} />
            </ActionIcon>
          </>
        )}

        {/* Main image with fade transition */}
        <Transition
          mounted={imageVisible}
          transition="fade"
          duration={300}
          timingFunction="ease"
        >
          {(transitionStyles) => (
            <Image
              src={currentImage.src}
              alt={currentImage.alt || `Image ${currentIndex + 1}`}
              fit="contain"
              style={transitionStyles}
              className={styles.imageContainer}
              fallbackSrc="data:image/svg+xml,%3csvg%20width='400'%20height='300'%20xmlns='http://www.w3.org/2000/svg'%3e%3crect%20width='400'%20height='300'%20fill='%23f8f9fa'/%3e%3ctext%20x='200'%20y='150'%20font-family='Arial'%20font-size='16'%20fill='%23868e96'%20text-anchor='middle'%20dominant-baseline='middle'%3eImage not found%3c/text%3e%3c/svg%3e"
            />
          )}
        </Transition>

        {/* Image info overlay */}
        {(currentImage.title || currentImage.metadata || images.length > 1) && (
          <div className={styles.infoOverlay}>
            <div className={styles.infoContent}>
              <div>
                {currentImage.title && (
                  <Text className={styles.infoTitle}>{currentImage.title}</Text>
                )}
                {currentImage.metadata && (
                  <Text className={styles.infoMetadata}>
                    {currentImage.metadata}
                  </Text>
                )}
              </div>
              {images.length > 1 && (
                <Text className={styles.infoCounter}>
                  {currentIndex + 1} / {images.length}
                </Text>
              )}
            </div>
          </div>
        )}
      </div>
    </Modal>
  );
};
