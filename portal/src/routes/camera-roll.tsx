import { createFileRoute } from "@tanstack/react-router";
import { CameraRoll } from "../components/CameraRoll";

export const Route = createFileRoute("/camera-roll")({
  component: CameraRollPage,
});

function CameraRollPage() {
  return <CameraRoll />;
}