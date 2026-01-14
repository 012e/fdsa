import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/code-editor-demo')({
  component: RouteComponent,
})

function RouteComponent() {
  return <div>Hello "/code-editor-demo"!</div>
}
