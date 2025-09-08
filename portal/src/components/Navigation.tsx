import React from "react";
import { Group, Button } from "@mantine/core";
import { Link, useLocation } from "@tanstack/react-router";

export const Navigation: React.FC = () => {
  const location = useLocation();

  return (
    <Group justify="center" gap="md" p="md">
      <Link to="/">
        <Button
          variant={location.pathname === "/" ? "filled" : "light"}
          size="sm"
        >
          Conversations
        </Button>
      </Link>
      <Link to="/camera-roll">
        <Button
          variant={location.pathname === "/camera-roll" ? "filled" : "light"}
          size="sm"
        >
          Camera Roll
        </Button>
      </Link>
    </Group>
  );
};
