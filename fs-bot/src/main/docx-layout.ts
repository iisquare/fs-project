import JSZip from 'jszip'

export const WORD_CONTENT_WIDTH_TWIPS = 11906 - 1080 * 2
export const WORD_CONTENT_WIDTH_EMU = WORD_CONTENT_WIDTH_TWIPS * 635
const WORD_BODY_FONTS =
  '<w:rFonts w:ascii="Arial" w:hAnsi="Arial" w:eastAsia="Microsoft YaHei" w:cs="Arial"/>'
const WORD_CODE_FONTS =
  '<w:rFonts w:ascii="Consolas" w:hAnsi="Consolas" w:eastAsia="Microsoft YaHei" w:cs="Consolas"/>'

function normalizeRunFonts(xml: string): string {
  return xml.replace(/<w:rFonts\b[^>]*\/>/g, (font) =>
    /Consolas|Courier|mononoki/i.test(font) ? WORD_CODE_FONTS : WORD_BODY_FONTS
  )
}

function normalizeRunSizes(xml: string): string {
  return xml.replace(/<w:p>[\s\S]*?<\/w:p>/g, (paragraph) => {
    const heading = /<w:pStyle\b[^>]*w:val="(?:Heading|Title)/.test(paragraph)
    return paragraph.replace(/<w:rPr>[\s\S]*?<\/w:rPr>/g, (properties) => {
      const code = /Consolas/i.test(properties)
      if (heading && !code) return properties
      const size = code ? 20 : 22
      let next = properties
        .replace(/<w:sz\b[^>]*\/>/g, `<w:sz w:val="${size}"/>`)
        .replace(/<w:szCs\b[^>]*\/>/g, `<w:szCs w:val="${size}"/>`)
      if (code) next = next.replace(/<w:i\b[^>]*\/>/g, '')
      return next
    })
  })
}

function normalizeRunShading(xml: string): string {
  return xml.replace(/<w:rPr>[\s\S]*?<\/w:rPr>/g, (properties) => {
    const code = /Consolas/i.test(properties)
    const hadInlineCodeShading = code && /<w:shd\b/.test(properties)
    let next = properties.replace(/<w:shd\b[^>]*\/>/g, '')
    if (hadInlineCodeShading) {
      next = next
        .replace(/<w:highlight\b[^>]*\/>/g, '')
        .replace('</w:rPr>', '<w:highlight w:val="lightGray"/></w:rPr>')
    }
    return next
  })
}

/**
 * html-to-docx 会忽略百分比表格宽度和图片宽度。
 * 生成后直接修正 OOXML：表格按正文页宽铺满，图片按原有比例拉伸到正文宽度。
 */
export async function normalizeDocxLayout(buffer: Buffer): Promise<Buffer> {
  const zip = await JSZip.loadAsync(buffer)
  const documentFile = zip.file('word/document.xml')
  if (!documentFile) return buffer
  let xml = await documentFile.async('string')
  xml = normalizeRunFonts(xml)
  xml = normalizeRunSizes(xml)
  xml = normalizeRunShading(xml)

  xml = xml.replace(/<w:tblPr>[\s\S]*?<\/w:tblPr>/g, (block) => {
    let next = block.replace(/<w:tblW\b[^>]*\/>/, '<w:tblW w:type="pct" w:w="5000"/>')
    if (!/<w:tblLayout\b/.test(next)) {
      next = next.replace('</w:tblPr>', '<w:tblLayout w:type="autofit"/></w:tblPr>')
    }
    return next
  })

  xml = xml.replace(/<w:tblGrid>([\s\S]*?)<\/w:tblGrid>/g, (_block, grid: string) => {
    const columnCount = (grid.match(/<w:gridCol\b/g) ?? []).length
    if (!columnCount) return _block
    const columnWidth = Math.floor(WORD_CONTENT_WIDTH_TWIPS / columnCount)
    let index = 0
    const columns = grid.replace(/<w:gridCol\b[^>]*\/>/g, () => {
      const current = ++index
      const width =
        current === columnCount
          ? WORD_CONTENT_WIDTH_TWIPS - columnWidth * (columnCount - 1)
          : columnWidth
      return `<w:gridCol w:w="${width}"/>`
    })
    return `<w:tblGrid>${columns}</w:tblGrid>`
  })

  xml = xml.replace(/<w:drawing>[\s\S]*?<\/w:drawing>/g, (drawing) => {
    const extent = drawing.match(/<wp:extent cx="(\d+)" cy="(\d+)"\s*\/>/)
    if (!extent) return drawing
    const width = Number(extent[1])
    const height = Number(extent[2])
    if (!width || !height) return drawing
    const targetHeight = Math.max(1, Math.round((height / width) * WORD_CONTENT_WIDTH_EMU))
    return drawing
      .replace(
        /<wp:extent cx="\d+" cy="\d+"\s*\/>/,
        `<wp:extent cx="${WORD_CONTENT_WIDTH_EMU}" cy="${targetHeight}"/>`
      )
      .replace(
        /<a:ext cx="\d+" cy="\d+"\s*\/>/g,
        `<a:ext cx="${WORD_CONTENT_WIDTH_EMU}" cy="${targetHeight}"/>`
      )
  })

  zip.file('word/document.xml', xml)
  const stylesFile = zip.file('word/styles.xml')
  if (stylesFile) {
    zip.file('word/styles.xml', normalizeRunFonts(await stylesFile.async('string')))
  }
  const normalized = await zip.generateAsync({
    type: 'nodebuffer',
    compression: 'DEFLATE'
  })
  return Buffer.from(normalized)
}
