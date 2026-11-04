import JSZip from 'jszip'

interface DocxElement {
  type?: string
  value?: string
  valueList?: DocxElement[]
  spacingBefore?: number
  spacingAfter?: number
}

export interface EditorDocumentState {
  version?: string
  data?: {
    main?: DocxElement[]
  }
  options?: any
}

interface ParagraphPlan {
  kind: 'paragraph' | 'table' | 'pageBreak'
  text: string
  spacingBefore?: number
  spacingAfter?: number
}

const DOCUMENT_PATH = 'word/document.xml'
const EDITOR_STATE_PATH = 'customXml/canvas-editor-state.json'
const CONTENT_TYPES_PATH = '[Content_Types].xml'
const WORD_NS = 'http://schemas.openxmlformats.org/wordprocessingml/2006/main'
const CONTENT_TYPES_NS = 'http://schemas.openxmlformats.org/package/2006/content-types'
const SPACING_ATTRIBUTES = new Set([
  'before',
  'beforeLines',
  'beforeAutospacing',
  'after',
  'afterLines',
  'afterAutospacing',
])

const isPageBreakElement = (element: Element) => {
  if (element.localName === 'pageBreakBefore') return true
  return Array.from(element.getElementsByTagNameNS(WORD_NS, 'br'))
    .some(item => item.getAttributeNS(WORD_NS, 'type') === 'page')
}

const paragraphText = (paragraph: Element) => {
  return Array.from(paragraph.getElementsByTagNameNS(WORD_NS, 't'))
    .map(item => item.textContent || '')
    .join('')
}

const directChildren = (element: Element) => {
  return Array.from(element.children)
}

const directChild = (element: Element, localName: string) => {
  return directChildren(element).find(item => item.localName === localName)
}

const ensureParagraphProperties = (document: XMLDocument, paragraph: Element) => {
  const current = directChild(paragraph, 'pPr')
  if (current) return current
  const properties = document.createElementNS(WORD_NS, 'w:pPr')
  paragraph.insertBefore(properties, paragraph.firstChild)
  return properties
}

const ensureSpacing = (document: XMLDocument, paragraph: Element) => {
  const properties = ensureParagraphProperties(document, paragraph)
  const current = directChild(properties, 'spacing')
  if (current) return current
  const spacing = document.createElementNS(WORD_NS, 'w:spacing')
  const index = directChildren(properties).findIndex(item => [
    'ind',
    'contextualSpacing',
    'jc',
    'outlineLvl',
    'rPr',
    'sectPr',
    'pPrChange',
  ].includes(item.localName || ''))
  properties.insertBefore(spacing, index < 0 ? null : properties.children[index])
  return spacing
}

const buildParagraphPlans = (elements: DocxElement[] = []) => {
  const plans: ParagraphPlan[] = []
  const createParagraph = (element: DocxElement): ParagraphPlan => {
    return {
      kind: 'paragraph',
      text: '',
      spacingBefore: element.spacingBefore,
      spacingAfter: element.spacingAfter,
    }
  }
  let current: ParagraphPlan | null = null
  const flushParagraph = () => {
    if (!current) return
    plans.push(current)
    current = null
  }

  for (const element of elements) {
    if (element.type === 'table') {
      flushParagraph()
      plans.push({ kind: 'table', text: '' })
      continue
    }
    if (element.type === 'pageBreak') {
      flushParagraph()
      plans.push({ kind: 'pageBreak', text: '' })
      continue
    }
    if (element.type === 'separator') {
      flushParagraph()
      plans.push({
        kind: 'paragraph',
        text: '',
        spacingBefore: element.spacingBefore,
        spacingAfter: element.spacingAfter,
      })
      continue
    }
    if (element.type === 'title') {
      flushParagraph()
      plans.push({
        kind: 'paragraph',
        text: (element.valueList || []).map(item => item.value || '').join(''),
        spacingBefore: element.spacingBefore,
        spacingAfter: element.spacingAfter,
      })
      continue
    }
    if (element.type === 'list') {
      flushParagraph()
      const lines = (element.valueList || []).map(item => item.value || '').join('')
        .split('\n')
        .filter((line, index) => index > 0 || line !== '')
      for (const line of lines) {
        plans.push({
          kind: 'paragraph',
          text: line,
          spacingBefore: element.spacingBefore,
          spacingAfter: element.spacingAfter,
        })
      }
      continue
    }

    const value = typeof element.value === 'string' ? element.value : ''
    if (!value) continue
    for (const char of value) {
      const paragraph: ParagraphPlan = current || createParagraph(element)
      current = paragraph
      if (char === '\n') {
        flushParagraph()
      } else {
        paragraph.text += char
      }
    }
  }
  flushParagraph()
  return plans
}

const createPageBreakParagraph = (document: XMLDocument) => {
  const paragraph = document.createElementNS(WORD_NS, 'w:p')
  const run = document.createElementNS(WORD_NS, 'w:r')
  const pageBreak = document.createElementNS(WORD_NS, 'w:br')
  pageBreak.setAttributeNS(WORD_NS, 'w:type', 'page')
  run.appendChild(pageBreak)
  paragraph.appendChild(run)
  return paragraph
}

const createEmptyParagraph = (document: XMLDocument, source?: Element) => {
  if (!source) return document.createElementNS(WORD_NS, 'w:p')
  return document.importNode(source, true) as Element
}

const applyParagraphSpacing = (
  document: XMLDocument,
  paragraph: Element,
  source?: Element,
  plan?: ParagraphPlan,
) => {
  const sourceProperties = source ? directChild(source, 'pPr') : undefined
  const sourceSpacing = sourceProperties ? directChild(sourceProperties, 'spacing') : undefined
  const targetSpacing = sourceSpacing || plan?.spacingBefore !== undefined || plan?.spacingAfter !== undefined
    ? ensureSpacing(document, paragraph)
    : directChild(directChild(paragraph, 'pPr') || paragraph, 'spacing')
  if (!targetSpacing) return

  if (sourceSpacing) {
    for (const attribute of Array.from(sourceSpacing.attributes)) {
      if (attribute.namespaceURI === WORD_NS && SPACING_ATTRIBUTES.has(attribute.localName)) {
        targetSpacing.setAttributeNS(WORD_NS, attribute.name, attribute.value)
      }
    }
  }
  if (plan?.spacingBefore !== undefined) {
    targetSpacing.setAttributeNS(WORD_NS, 'w:before', String(Math.round(plan.spacingBefore * 15)))
  }
  if (plan?.spacingAfter !== undefined) {
    targetSpacing.setAttributeNS(WORD_NS, 'w:after', String(Math.round(plan.spacingAfter * 15)))
  }
}

const restoreParagraphLayout = (
  document: XMLDocument,
  body: Element,
  plans: ParagraphPlan[],
  sourceBody?: Element,
) => {
  const bodyNodes = directChildren(body)
    .filter(node => ['p', 'tbl'].includes(node.localName || ''))
  const sourceParagraphs = sourceBody
    ? Array.from(sourceBody.children).filter(node => node.localName === 'p')
    : []
  const sourceEmptyParagraphs = sourceParagraphs.filter(paragraph => {
    return !paragraphText(paragraph) && !isPageBreakElement(paragraph)
  })
  let cursor = 0

  for (const plan of plans) {
    if (plan.kind === 'pageBreak') {
      const current = bodyNodes[cursor]
      if (current?.localName === 'p' && isPageBreakElement(current)) {
        cursor++
        continue
      }
      const paragraph = createPageBreakParagraph(document)
      const reference = current || directChild(body, 'sectPr')
      body.insertBefore(paragraph, reference || null)
      bodyNodes.splice(cursor, 0, paragraph)
      cursor++
      continue
    }
    if (plan.kind === 'table') {
      while (cursor < bodyNodes.length && bodyNodes[cursor].localName !== 'tbl') cursor++
      if (cursor < bodyNodes.length) cursor++
      continue
    }

    const current = bodyNodes[cursor]
    if (current?.localName === 'p' && (plan.text || !paragraphText(current))) {
      cursor++
      continue
    }
    const source = !plan.text ? sourceEmptyParagraphs.shift() : undefined
    const paragraph = createEmptyParagraph(document, source)
    applyParagraphSpacing(document, paragraph, source, plan)
    const reference = current || directChild(body, 'sectPr')
    body.insertBefore(paragraph, reference || null)
    bodyNodes.splice(cursor, 0, paragraph)
    cursor++
  }

  if (!sourceBody) return
  const sourceByText = new Map<string, Element[]>()
  for (const paragraph of sourceBody.getElementsByTagNameNS(WORD_NS, 'p')) {
    const text = paragraphText(paragraph)
    const list = sourceByText.get(text) || []
    list.push(paragraph)
    sourceByText.set(text, list)
  }
  for (const paragraph of body.getElementsByTagNameNS(WORD_NS, 'p')) {
    const text = paragraphText(paragraph)
    const source = sourceByText.get(text)?.shift()
    applyParagraphSpacing(document, paragraph, source)
  }
}

const modifyDocumentXml = (sourceXml: string, exportedXml: string, state?: EditorDocumentState) => {
  const parser = new DOMParser()
  const sourceDocument = parser.parseFromString(sourceXml, 'application/xml')
  const exportedDocument = parser.parseFromString(exportedXml, 'application/xml')
  const exportedBody = exportedDocument.getElementsByTagNameNS(WORD_NS, 'body')[0]
  if (!exportedBody) return exportedXml
  const sourceBody = sourceDocument.getElementsByTagNameNS(WORD_NS, 'body')[0]
  const plans = buildParagraphPlans(state?.data?.main)
  restoreParagraphLayout(exportedDocument, exportedBody, plans, sourceBody)
  return new XMLSerializer().serializeToString(exportedDocument)
}

const registerContentType = async (zip: JSZip, partName: string, contentType: string) => {
  const file = zip.file(CONTENT_TYPES_PATH)
  if (!file) throw new Error('Word 文件缺少内容类型清单')
  const parser = new DOMParser()
  const document = parser.parseFromString(await file.async('string'), 'application/xml')
  const normalizedPartName = `/${partName.replace(/^\/+/, '')}`
  let override = Array.from(document.getElementsByTagNameNS(CONTENT_TYPES_NS, 'Override'))
    .find(item => item.getAttribute('PartName') === normalizedPartName)
  if (!override) {
    override = document.createElementNS(CONTENT_TYPES_NS, 'Override')
    document.documentElement.appendChild(override)
  }
  override.setAttribute('PartName', normalizedPartName)
  override.setAttribute('ContentType', contentType)
  zip.file(CONTENT_TYPES_PATH, new XMLSerializer().serializeToString(document), {
    compression: 'DEFLATE',
  })
}

const DocxLayoutUtil = {
  async extractEditorState(source: ArrayBuffer): Promise<EditorDocumentState | undefined> {
    try {
      const zip = await JSZip.loadAsync(source)
      const file = zip.file(EDITOR_STATE_PATH)
      return file ? JSON.parse(await file.async('string')) : undefined
    } catch (error) {
      console.warn('读取编辑器状态失败，继续从 Word 文件恢复', error)
      return undefined
    }
  },
  async preserveEditorLayout(
    exportedBlob: Blob,
    source: ArrayBuffer | undefined,
    state?: EditorDocumentState,
  ): Promise<Blob> {
    try {
      const exported = await JSZip.loadAsync(exportedBlob)
      const exportedFile = exported.file(DOCUMENT_PATH)
      if (!exportedFile) return exportedBlob
      const sourceFile = source
        ? (await JSZip.loadAsync(source)).file(DOCUMENT_PATH)
        : null
      const sourceXml = sourceFile ? await sourceFile.async('string') : '<w:document xmlns:w="' + WORD_NS + '"><w:body/></w:document>'
      const exportedXml = await exportedFile.async('string')
      exported.file(DOCUMENT_PATH, modifyDocumentXml(sourceXml, exportedXml, state), {
        compression: 'DEFLATE',
      })
      if (state) {
        exported.file(EDITOR_STATE_PATH, JSON.stringify(state), {
          compression: 'DEFLATE',
        })
        await registerContentType(exported, EDITOR_STATE_PATH, 'application/json')
      }
      return exported.generateAsync({
        type: 'blob',
        mimeType: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
        compression: 'DEFLATE',
      })
    } catch (error) {
      console.warn('保留 Word 段落布局失败，继续使用原始导出文件', error)
      return exportedBlob
    }
  },
}

export default DocxLayoutUtil
