import { CodeEditor, detectLanguageFromPath } from './code-editor'

interface CodeViewerProps {
  code: string
  language?: string
  fileName?: string
  height?: string
  showLineNumbers?: boolean
  highlightLines?: number[]
}

export function CodeViewer({
  code,
  language,
  fileName,
  height = '300px',
  showLineNumbers = true,
  highlightLines = [],
}: CodeViewerProps) {
  const detectedLanguage = language || (fileName ? detectLanguageFromPath(fileName) : 'plaintext')

  return (
    <CodeEditor
      value={code}
      onChange={() => {}} // No-op for read-only
      language={detectedLanguage}
      height={height}
      readOnly={true}
      options={{
        readOnly: true,
        minimap: { enabled: false },
        lineNumbers: showLineNumbers ? 'on' : 'off',
        scrollBeyondLastLine: false,
        renderLineHighlight: 'none',
        contextmenu: false,
        glyphMargin: false,
        folding: false,
        lineDecorationsWidth: 0,
        lineNumbersMinChars: 3,
      }}
    />
  )
}
