import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/editor-demo')({
  component: RouteComponent,
})

function RouteComponent() {
  return <div>Hello "/editor-demo"!</div>
}
