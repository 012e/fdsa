import { createFileRoute, redirect } from '@tanstack/react-router'

export const Route = createFileRoute('/')({
  // 1. Handle the redirect before the route loads
  beforeLoad: () => {
    throw redirect({
      to: '/repositories',
      replace: true, // Replaces the history entry so the back button works correctly
    })
  },
  // 2. Render nothing if the component is reached
  component: () => <div></div>,
})