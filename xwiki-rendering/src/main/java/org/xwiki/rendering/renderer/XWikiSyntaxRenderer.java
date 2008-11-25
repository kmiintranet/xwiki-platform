/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.rendering.renderer;

import java.util.Map;
import java.util.Stack;

import org.xwiki.rendering.internal.renderer.XWikiMacroPrinter;
import org.xwiki.rendering.internal.renderer.XWikiSyntaxImageRenderer;
import org.xwiki.rendering.internal.renderer.XWikiSyntaxLinkRenderer;
import org.xwiki.rendering.listener.Image;
import org.xwiki.rendering.listener.LinkType;
import org.xwiki.rendering.listener.ListType;
import org.xwiki.rendering.listener.SectionLevel;
import org.xwiki.rendering.listener.Link;
import org.xwiki.rendering.listener.Format;
import org.xwiki.rendering.listener.xml.XMLNode;
import org.apache.commons.lang.StringUtils;

/**
 * Generates XWiki Syntax from {@link org.xwiki.rendering.block.XDOM}. This is useful for example to convert other wiki
 * syntaxes to the XWiki syntax. It's also useful in our tests to verify that round tripping from XWiki Syntax to the
 * DOM and back to XWiki Syntax generates the same content as the initial syntax.
 * 
 * @version $Id$
 * @since 1.5M2
 */
public class XWikiSyntaxRenderer extends AbstractPrintRenderer
{
    private XWikiSyntaxLinkRenderer linkRenderer;

    private XWikiSyntaxImageRenderer imageRenderer;
    
    private boolean isFirstElementRendered = false;

    private StringBuffer listStyle = new StringBuffer();

    private boolean isBeginListItemFound = false;

    private boolean isEndListItemFound = false;

    private boolean isBeginDefinitionListItemFound = false;

    private boolean isEndDefinitionListItemFound = false;

    private boolean isBeginQuotationLineFound = false;

    private boolean isEndQuotationLineFound = false;

    private int listDepth = 0;

    private int definitionListDepth = 0;

    private int quotationDepth = 0;

    private Stack<Boolean> isEndTableRowFoundStack = new Stack<Boolean>();

    private XWikiMacroPrinter macroPrinter;

    private WikiPrinter linkBlocksPrinter;

    private Map<String, String> previousFormatParameters;

    public XWikiSyntaxRenderer(WikiPrinter printer)
    {
        super(printer);

        this.macroPrinter = new XWikiMacroPrinter();
        this.linkRenderer = new XWikiSyntaxLinkRenderer();
        this.imageRenderer = new XWikiSyntaxImageRenderer();
    }

    /**
     * {@inheritDoc}
     * 
     * @see PrintRenderer#beginDocument()
     */
    public void beginDocument()
    {
        // Nothing to do
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.PrintRenderer#endDocument()
     */
    public void endDocument()
    {
        // Don't do anything
    }

    /**
     * {@inheritDoc}
     * 
     * @see PrintRenderer#beginLink(Link, boolean, Map)
     */
    public void beginLink(Link link, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        this.linkRenderer.beginRenderLink(getPrinter(), link, isFreeStandingURI, parameters);
        
        // Defer printing the link content since we need to gather all nested elements
        this.linkBlocksPrinter = new DefaultWikiPrinter();
        pushPrinter(this.linkBlocksPrinter);
    }

    /**
     * {@inheritDoc}
     * 
     * @see PrintRenderer#endLink(Link, boolean, Map)
     */
    public void endLink(Link link, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        String content = this.linkBlocksPrinter.toString();
        popPrinter();

        this.linkRenderer.renderLinkContent(getPrinter(), content);
        this.linkRenderer.endRenderLink(getPrinter(), link, isFreeStandingURI, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Renderer#beginFormat(Format, Map)
     */
    public void beginFormat(Format format, Map<String, String> parameters)
    {
        switch (format) {
            case BOLD:
                print("**");
                break;
            case ITALIC:
                print("//");
                break;
            case STRIKEDOUT:
                print("--");
                break;
            case UNDERLINED:
                print("__");
                break;
            case SUPERSCRIPT:
                print("^^");
                break;
            case SUBSCRIPT:
                print(",,");
                break;
            case MONOSPACE:
                print("##");
                break;
        }
        // If the previous format had parameters and the parameters are different from the current ones then close them
        if (this.previousFormatParameters != null && !this.previousFormatParameters.equals(parameters)) {
            this.previousFormatParameters = null;
            printParameters(parameters, false);
        } else if (this.previousFormatParameters == null) {
            this.previousFormatParameters = null;
            printParameters(parameters, false);
        } else {
            this.previousFormatParameters = null;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Renderer#endFormat(Format, Map)
     */
    public void endFormat(Format format, Map<String, String> parameters)
    {
        switch (format) {
            case BOLD:
                print("**");
                break;
            case ITALIC:
                print("//");
                break;
            case STRIKEDOUT:
                print("--");
                break;
            case UNDERLINED:
                print("__");
                break;
            case SUPERSCRIPT:
                print("^^");
                break;
            case SUBSCRIPT:
                print(",,");
                break;
            case MONOSPACE:
                print("##");
                break;
        }
        if (!parameters.isEmpty()) {
            this.previousFormatParameters = parameters;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.PrintRenderer#beginParagraph(java.util.Map)
     */
    public void beginParagraph(Map<String, String> parameters)
    {
        printNewLine();
        printParameters(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.PrintRenderer#endParagraph(java.util.Map)
     */
    public void endParagraph(Map<String, String> parameters)
    {
        this.previousFormatParameters = null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.PrintRenderer#onLineBreak()
     */
    public void onLineBreak()
    {
        print("\n");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.PrintRenderer#onNewLine()
     */
    public void onNewLine()
    {
        print("\\");
    }

    /**
     * {@inheritDoc}
     * 
     * @see PrintRenderer#onInlineMacro(String, java.util.Map, String)
     */
    public void onInlineMacro(String name, Map<String, String> parameters, String content)
    {
        print(this.macroPrinter.print(name, parameters, content));
    }

    /**
     * {@inheritDoc}
     * 
     * @see PrintRenderer#onStandaloneMacro(String, java.util.Map, String)
     */
    public void onStandaloneMacro(String name, Map<String, String> parameters, String content)
    {
        printNewLine();
        print(this.macroPrinter.print(name, parameters, content));
    }

    /**
     * {@inheritDoc}
     * 
     * @see PrintRenderer#beginSection(SectionLevel, Map)
     */
    public void beginSection(SectionLevel level, Map<String, String> parameters)
    {
        printNewLine();
        printParameters(parameters);
        print(StringUtils.repeat("=", level.getAsInt()) + " ");
    }

    /**
     * {@inheritDoc}
     * 
     * @see PrintRenderer#endSection(SectionLevel, Map)
     */
    public void endSection(SectionLevel level, Map<String, String> parameters)
    {
        print(" " + StringUtils.repeat("=", level.getAsInt()));
    }

    /**
     * {@inheritDoc}
     * 
     * @see PrintRenderer#onWord(String)
     */
    public void onWord(String word)
    {
        print(word);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.PrintRenderer#onSpace()
     */
    public void onSpace()
    {
        print(" ");
    }

    /**
     * {@inheritDoc}
     * 
     * @see PrintRenderer#onSpecialSymbol(char)
     */
    public void onSpecialSymbol(char symbol)
    {
        print("" + symbol);
    }

    /**
     * {@inheritDoc}
     * 
     * @see PrintRenderer#onEscape(String)
     */
    public void onEscape(String escapedString)
    {
        for (int i = 0; i < escapedString.length(); i++) {
            print("~" + escapedString.charAt(i));
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see PrintRenderer#beginList(org.xwiki.rendering.listener.ListType, java.util.Map)
     */
    public void beginList(ListType listType, Map<String, String> parameters)
    {
        if (this.isBeginListItemFound && !this.isEndListItemFound) {
            print("\n");
            this.isBeginListItemFound = false;
        } else {
            printNewLine();
        }

        if (listType == ListType.BULLETED) {
            this.listStyle.append("*");
        } else {
            this.listStyle.append("1");
        }
        printParameters(parameters);

        this.listDepth++;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.PrintRenderer#beginListItem()
     */
    public void beginListItem()
    {
        if (this.isEndListItemFound) {
            print("\n");
            this.isEndListItemFound = false;
            this.isBeginListItemFound = false;
        }

        this.isBeginListItemFound = true;

        print(this.listStyle.toString());
        if (this.listStyle.charAt(0) == '1') {
            print(".");
        }
        print(" ");
    }

    /**
     * {@inheritDoc}
     * 
     * @see PrintRenderer#endList(org.xwiki.rendering.listener.ListType, java.util.Map)
     */
    public void endList(ListType listType, Map<String, String> parameters)
    {
        this.listStyle.setLength(this.listStyle.length() - 1);
        this.listDepth--;
        if (this.listDepth == 0) {
            this.isBeginListItemFound = false;
            this.isEndListItemFound = false;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.PrintRenderer#endListItem()
     */
    public void endListItem()
    {
        this.isEndListItemFound = true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.Renderer#beginXMLNode(XMLNode)
     */
    public void beginXMLNode(XMLNode node)
    {
        // There's no xwiki wiki syntax for writing HTML (we have to use Macros for that). Hence discard
        // any XML node events.
    }

    /**
     * {@inheritDoc}
     * 
     * @see PrintRenderer#endXMLNode(XMLNode)
     */
    public void endXMLNode(XMLNode node)
    {
        // There's no xwiki wiki syntax for writing HTML (we have to use Macros for that). Hence discard
        // any XML node events.
    }

    /**
     * {@inheritDoc}
     * 
     * @see PrintRenderer#beginMacroMarker(String, java.util.Map, String)
     */
    public void beginMacroMarker(String name, Map<String, String> parameters, String content)
    {
        // When we encounter a macro marker we ignore all other blocks inside since we're going to use the macro
        // definition wrapped by the macro marker to construct the xwiki syntax.
        pushVoidPrinter();
    }

    /**
     * {@inheritDoc}
     * 
     * @see PrintRenderer#endMacroMarker(String, java.util.Map, String)
     */
    public void endMacroMarker(String name, Map<String, String> parameters, String content)
    {
        popVoidPrinter();

        print(this.macroPrinter.print(name, parameters, content));
    }

    /**
     * {@inheritDoc}
     * 
     * @see PrintRenderer#onId(String)
     */
    public void onId(String name)
    {
        print("{{id name=\"" + name + "\"}}");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.Renderer#onHorizontalLine(Map)
     */
    public void onHorizontalLine(Map<String, String> parameters)
    {
        printNewLine();
        printParameters(parameters);
        print("----");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.Renderer#onVerbatimInline(String)
     */
    public void onVerbatimInline(String protectedString)
    {
        print("{{{" + protectedString + "}}}");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.Renderer#onVerbatimStandalone(String, Map)
     */
    public void onVerbatimStandalone(String protectedString, Map<String, String> parameters)
    {
        printNewLine();
        printParameters(parameters);
        onVerbatimInline(protectedString);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.Renderer#onEmptyLines(int)
     */
    public void onEmptyLines(int count)
    {
        print(StringUtils.repeat("\n", count));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginDefinitionList()
     * @since 1.6M2
     */
    public void beginDefinitionList()
    {
        if (this.isBeginListItemFound && !this.isEndListItemFound) {
            print("\n");
            // - we are inside an existing definition list
        } else if (this.isBeginDefinitionListItemFound && !this.isEndDefinitionListItemFound) {
            print("\n");
            this.isBeginDefinitionListItemFound = false;
        } else {
            printNewLine();
        }

        this.definitionListDepth++;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endDefinitionList()
     * @since 1.6M2
     */
    public void endDefinitionList()
    {
        this.definitionListDepth--;
        if (this.definitionListDepth == 0) {
            this.isBeginDefinitionListItemFound = false;
            this.isEndDefinitionListItemFound = false;
            this.isBeginListItemFound = false;
            this.isEndDefinitionListItemFound = false;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginDefinitionTerm()
     * @since 1.6M2
     */
    public void beginDefinitionTerm()
    {
        if (this.isEndDefinitionListItemFound) {
            print("\n");
            this.isEndDefinitionListItemFound = false;
            this.isBeginDefinitionListItemFound = false;
        }
        this.isBeginDefinitionListItemFound = true;

        if (this.listStyle.length() > 0) {
            print(this.listStyle.toString());
            if (this.listStyle.charAt(0) == '1') {
                print(".");
            }
        }
        print(StringUtils.repeat(":", this.definitionListDepth - 1));
        print("; ");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginDefinitionDescription()
     * @since 1.6M2
     */
    public void beginDefinitionDescription()
    {
        if (this.isEndDefinitionListItemFound) {
            print("\n");
            this.isEndDefinitionListItemFound = false;
            this.isBeginDefinitionListItemFound = false;
        }
        this.isBeginDefinitionListItemFound = true;

        if (this.listStyle.length() > 0) {
            print(this.listStyle.toString());
            if (this.listStyle.charAt(0) == '1') {
                print(".");
            }
        }
        print(StringUtils.repeat(":", this.definitionListDepth - 1));
        print(": ");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endDefinitionTerm()
     * @since 1.6M2
     */
    public void endDefinitionTerm()
    {
        this.isEndDefinitionListItemFound = true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endDefinitionDescription()
     * @since 1.6M2
     */
    public void endDefinitionDescription()
    {
        this.isEndDefinitionListItemFound = true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginQuotation(java.util.Map)
     * @since 1.6M2
     */
    public void beginQuotation(Map<String, String> parameters)
    {
        if (this.isBeginQuotationLineFound && !this.isEndQuotationLineFound) {
            print("\n");
            this.isBeginQuotationLineFound = false;
        } else {
            printNewLine();
        }

        if (!parameters.isEmpty()) {
            printParameters(parameters);
        }

        this.quotationDepth++;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endQuotation(java.util.Map)
     * @since 1.6M2
     */
    public void endQuotation(Map<String, String> parameters)
    {
        this.quotationDepth--;
        if (this.quotationDepth == 0) {
            this.isBeginQuotationLineFound = false;
            this.isEndQuotationLineFound = false;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginQuotationLine()
     * @since 1.6M2
     */
    public void beginQuotationLine()
    {
        if (this.isEndQuotationLineFound) {
            print("\n");
            this.isEndQuotationLineFound = false;
            this.isBeginQuotationLineFound = false;
        }
        this.isBeginQuotationLineFound = true;

        print(StringUtils.repeat(">", this.quotationDepth));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endQuotationLine()
     * @since 1.6M2
     */
    public void endQuotationLine()
    {
        this.isEndQuotationLineFound = true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginTable(java.util.Map)
     */
    public void beginTable(Map<String, String> parameters)
    {
        printNewLine();
        if (!parameters.isEmpty()) {
            printParameters(parameters);
        }

        this.isEndTableRowFoundStack.push(false);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginTableCell(java.util.Map)
     */
    public void beginTableCell(Map<String, String> parameters)
    {
        print("|");
        printParameters(parameters, false);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginTableHeadCell(java.util.Map)
     */
    public void beginTableHeadCell(Map<String, String> parameters)
    {
        print("|=");
        printParameters(parameters, false);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginTableRow(java.util.Map)
     */
    public void beginTableRow(Map<String, String> parameters)
    {
        if (this.isEndTableRowFoundStack.peek()) {
            print("\n");
        }

        printParameters(parameters, false);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endTable(java.util.Map)
     */
    public void endTable(Map<String, String> parameters)
    {
        this.isEndTableRowFoundStack.pop();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endTableCell(java.util.Map)
     */
    public void endTableCell(Map<String, String> parameters)
    {
        this.previousFormatParameters = null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endTableHeadCell(java.util.Map)
     */
    public void endTableHeadCell(Map<String, String> parameters)
    {
        this.previousFormatParameters = null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endTableRow(java.util.Map)
     */
    public void endTableRow(Map<String, String> parameters)
    {
        this.isEndTableRowFoundStack.set(this.isEndTableRowFoundStack.size() - 1, true);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onImage(org.xwiki.rendering.listener.Image, boolean, Map)
     */
    public void onImage(Image image, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        Link link = new Link();
        link.setReference("image:" + this.imageRenderer.renderImage(image));
        link.setType(LinkType.URI);
        
        this.linkRenderer.beginRenderLink(getPrinter(), link, isFreeStandingURI, parameters);
        this.linkRenderer.endRenderLink(getPrinter(), link, isFreeStandingURI, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginError(String, String)
     * @since 1.7M3
     */
    public void beginError(String message, String description)
    {
        // Don't do anything since we don't want errors to be visible in XWiki syntax.
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endError(String, String)
     * @since 1.7M3
     */
    public void endError(String message, String description)
    {
        // Don't do anything since we don't want errors to be visible in XWiki syntax.
    }

    protected void printParameters(Map<String, String> parameters)
    {
        printParameters(parameters, true);
    }

    protected void printParameters(Map<String, String> parameters, boolean newLine)
    {
        if (!parameters.isEmpty()) {
            StringBuffer buffer = new StringBuffer("(%");
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                buffer.append(' ').append(entry.getKey()).append('=').append('\"').append(entry.getValue())
                    .append('\"');
            }
            buffer.append(" %)");

            if (newLine) {
                buffer.append("\n");
            }

            print(buffer.toString());
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.AbstractPrintRenderer#print(java.lang.String)
     */
    @Override
    protected void print(String text)
    {
        // Handle empty formatting parameters.
        if (this.previousFormatParameters != null) {
            super.print("(%%)");
            this.previousFormatParameters = null;
        }

        super.print(text);
    }

    private void printNewLine()
    {
        if (this.isFirstElementRendered) {
            print("\n\n");
        } else {
            this.isFirstElementRendered = true;
        }
    }
}
