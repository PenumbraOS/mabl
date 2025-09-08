import { useState, useCallback } from "react";
import type { GalleryImage } from "../components/ImageGallery";

export const useImageGallery = () => {
  const [opened, setOpened] = useState(false);
  const [images, setImages] = useState<GalleryImage[]>([]);
  const [initialIndex, setInitialIndex] = useState(0);

  const openGallery = useCallback(
    (galleryImages: GalleryImage[], startIndex: number = 0) => {
      setImages(galleryImages);
      setInitialIndex(startIndex);
      setOpened(true);
    },
    []
  );

  const closeGallery = useCallback(() => {
    setOpened(false);
  }, []);

  return {
    opened,
    images,
    initialIndex,
    openGallery,
    closeGallery,
  };
};
