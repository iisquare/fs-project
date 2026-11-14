import JSZip from 'jszip'
import { describe, expect, it } from 'vitest'
import {
  normalizeDocxLayout,
  WORD_CONTENT_WIDTH_EMU,
  WORD_CONTENT_WIDTH_TWIPS
} from './docx-layout'

describe('normalizeDocxLayout', () => {
  it('fills the content width with tables and images', async () => {
    const zip = new JSZip()
    zip.file(
      'word/document.xml',
      `<w:document><w:body>
        <w:tbl>
          <w:tblPr><w:tblW w:type="dxa" w:w="8640"/></w:tblPr>
          <w:tblGrid><w:gridCol w:w="4320"/><w:gridCol w:w="4320"/></w:tblGrid>
        </w:tbl>
        <w:drawing>
          <wp:extent cx="1000" cy="500"/>
          <a:ext cx="1000" cy="500"/>
        </w:drawing>
        <w:p><w:r><w:rPr>
          <w:rFonts w:ascii="Inter" w:hAnsi="Inter"/>
          <w:sz w:val="23"/>
        </w:rPr><w:t>正文</w:t></w:r></w:p>
        <w:p><w:r><w:rPr>
          <w:rFonts w:ascii="Consolas" w:hAnsi="Consolas"/>
          <w:sz w:val="23"/><w:i/>
          <w:shd w:val="clear" w:fill="f2f3f5"/>
        </w:rPr><w:t>parent_id = 0</w:t></w:r></w:p>
      </w:body></w:document>`
    )
    const source = await zip.generateAsync({ type: 'nodebuffer' })
    const result = await normalizeDocxLayout(source)
    const normalized = await JSZip.loadAsync(result)
    const xml = await normalized.file('word/document.xml')!.async('string')

    expect(xml).toContain('<w:tblW w:type="pct" w:w="5000"/>')
    expect(xml).toContain('<w:tblLayout w:type="autofit"/>')
    expect(xml.match(/<w:gridCol w:w="\d+"\/>/g)).toEqual([
      `<w:gridCol w:w="${Math.floor(WORD_CONTENT_WIDTH_TWIPS / 2)}"/>`,
      `<w:gridCol w:w="${Math.ceil(WORD_CONTENT_WIDTH_TWIPS / 2)}"/>`
    ])
    expect(xml).toContain(
      `<wp:extent cx="${WORD_CONTENT_WIDTH_EMU}" cy="${WORD_CONTENT_WIDTH_EMU / 2}"/>`
    )
    expect(xml).toContain(
      `<a:ext cx="${WORD_CONTENT_WIDTH_EMU}" cy="${WORD_CONTENT_WIDTH_EMU / 2}"/>`
    )
    expect(xml).toContain('<w:highlight w:val="lightGray"/>')
    expect(xml).not.toContain('<w:shd')
    expect(xml).toContain(
      '<w:rFonts w:ascii="Arial" w:hAnsi="Arial" w:eastAsia="Microsoft YaHei" w:cs="Arial"/>'
    )
    expect(xml).toContain(
      '<w:rFonts w:ascii="Consolas" w:hAnsi="Consolas" w:eastAsia="Microsoft YaHei" w:cs="Consolas"/>'
    )
    expect(xml).not.toContain('w:ascii="Inter"')
    expect(xml).toContain('<w:sz w:val="22"/>')
    expect(xml).toContain('<w:sz w:val="20"/>')
    expect(xml).not.toContain('<w:i/>')
  })
})
