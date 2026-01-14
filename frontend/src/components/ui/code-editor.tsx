import { useRef, useState, useEffect } from 'react'
import Editor, { OnMount } from '@monaco-editor/react'
import { editor } from 'monaco-editor'

interface CodeEditorProps {
  value: string
  onChange: (value: string) => void
  language?: string
  height?: string
  theme?: 'vs-dark' | 'light' | 'vs' | 'hc-black' | 'auto'
  readOnly?: boolean
  options?: editor.IStandaloneEditorConstructionOptions
}

export function CodeEditor({
  value,
  onChange,
  language = 'plaintext',
  height = '400px',
  theme = 'auto',
  readOnly = false,
  options = {},
}: CodeEditorProps) {
  const editorRef = useRef<editor.IStandaloneCodeEditor | null>(null)
  const [isEditorReady, setIsEditorReady] = useState(false)
  const [currentTheme, setCurrentTheme] = useState<'vs-dark' | 'light' | 'vs' | 'hc-black'>('vs-dark')

  // Auto-detect system theme preference
  useEffect(() => {
    if (theme === 'auto') {
      const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
      const updateTheme = () => {
        setCurrentTheme(mediaQuery.matches ? 'vs-dark' : 'light')
      }
      
      updateTheme()
      mediaQuery.addEventListener('change', updateTheme)
      
      return () => mediaQuery.removeEventListener('change', updateTheme)
    } else {
      setCurrentTheme(theme as 'vs-dark' | 'light' | 'vs' | 'hc-black')
    }
  }, [theme])

  const handleEditorDidMount: OnMount = (editor, monaco) => {
    editorRef.current = editor
    setIsEditorReady(true)

    // Configure editor with enhanced syntax highlighting
    editor.updateOptions({
      minimap: { enabled: true },
      fontSize:14,
      lineNumbers: 'on',
      scrollBeyondLastLine: false,
      automaticLayout: true,
      bracketPairColorization: {
        enabled: true,
      },
      guides: {
        bracketPairs: true,
        indentation: true,
      },
      suggest: {
        showKeywords: true,
        showSnippets: true,
      },
      semanticHighlighting: {
        enabled: true,
      },
      ...options,
    })
  }

  const handleEditorChange = (value: string | undefined) => {
    onChange(value || '')
  }

  return (
    <div className="border rounded-md overflow-hidden">
      <Editor
        height={height}
        language={language}
        value={value}
        theme={theme}
        onChange={handleEditorChange}
        onMount={handleEditorDidMount}
        options={{
          readOnly,
          ...options,
        }}
        loading={
          <div className="flex items-center justify-center h-full">
            <div className="text-muted-foreground">Loading editor...</div>
          </div>
        }
      />
    </div>
  )
}

// Helper function to detect language from file path
export function detectLanguageFromPath(filePath: string): string {
  const extension = filePath.split('.').pop()?.toLowerCase()
  
  const languageMap: Record<string, string> = {
    // Web
    js: 'javascript',
    jsx: 'javascript',
    ts: 'typescript',
    tsx: 'typescript',
    html: 'html',
    css: 'css',
    scss: 'scss',
    sass: 'sass',
    less: 'less',
    json: 'json',
    
    // Backend
    java: 'java',
    py: 'python',
    rb: 'ruby',
    php: 'php',
    go: 'go',
    rs: 'rust',
    c: 'c',
    cpp: 'cpp',
    cs: 'csharp',
    
    // Markup/Config
    xml: 'xml',
    yaml: 'yaml',
    yml: 'yaml',
    toml: 'toml',
    md: 'markdown',
    
    // Shell
    sh: 'shell',
    bash: 'shell',
    zsh: 'shell',
    
    // Database
    sql: 'sql',
    
    // Other
    graphql: 'graphql',
    dockerfile: 'dockerfile',
  }

  return languageMap[extension || ''] || 'plaintext'
}
