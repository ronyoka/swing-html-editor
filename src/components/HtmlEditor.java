package components;

import com.sun.istack.internal.Nullable;

import javax.swing.*;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.CaretListener;
import javax.swing.event.ChangeListener;
import javax.swing.text.*;
import javax.swing.text.html.CSS;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.*;
import java.awt.event.ItemListener;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;

public class HtmlEditor extends JFrame {
    public static final String PIXEL = "px";
    private final HTMLEditorKit htmlEditorKit = new HTMLEditorKit();
    private final HTMLDocument htmlDocument = (HTMLDocument) htmlEditorKit.createDefaultDocument();
    private final ChangeListener fontSizeChangeListener;
    private final ItemListener fontFamilyItemListener;
    private final CaretListener htmlDocumentCaretListener;
    private Consumer<String> saveConsumer;
    private JPanel mainPanel;
    private JButton boldButton;
    private JButton underlineButton;
    private JButton italicButton;
    private JEditorPane htmlEditorPane;
    private JComboBox<String> fontFamilyCombo;
    private JButton alignLeftButton;
    private JButton alignCenter;
    private JButton alignRight;
    private JSpinner fontSizeSpinner;
    private JButton alignJustify;
    private JButton foregroundColorButton;
    private JButton backgroundColorButton;
    private JTabbedPane tabs;
    private JTextPane sourceTextPane;
    private JButton saveButton;
    private JButton cancelButton;
    private JLabel foregroundLabel;
    private JLabel backgroundLabel;
    private JPanel htmlEditorContainer;

    public HtmlEditor() {
        super("Html Editor");
        boldButton.addActionListener(e -> setSelectedTextCharacterAttributes(StyleConstants.FontConstants.Bold, this::isBold));
        underlineButton.addActionListener(e -> setSelectedTextCharacterAttributes(StyleConstants.FontConstants.Underline, this::isUnderline));
        italicButton.addActionListener(e -> setSelectedTextCharacterAttributes(StyleConstants.FontConstants.Italic, this::isItalic));
        alignLeftButton.addActionListener(e -> setParagraphAttributes(StyleConstants.Alignment, StyleConstants.ALIGN_LEFT));
        alignCenter.addActionListener(e -> setParagraphAttributes(StyleConstants.Alignment, StyleConstants.ALIGN_CENTER));
        alignRight.addActionListener(e -> setParagraphAttributes(StyleConstants.Alignment, StyleConstants.ALIGN_RIGHT));
        alignJustify.addActionListener(e -> setParagraphAttributes(StyleConstants.Alignment, StyleConstants.ALIGN_JUSTIFIED));
        foregroundColorButton.addActionListener(e -> {
            Color choice = JColorChooser.showDialog(foregroundColorButton, "Choose foreground color", foregroundColorButton.getBackground());
            if (choice != null) {
                foregroundColorButton.setForeground(choice);
                foregroundColorButton.setBackground(choice);
                setParagraphAttributes(StyleConstants.Foreground, choice);
            }
        });
        backgroundColorButton.addActionListener(e -> {
            Color choice = JColorChooser.showDialog(backgroundColorButton, "Choose background color", backgroundColorButton.getBackground());
            if (choice != null) {
                backgroundColorButton.setForeground(choice);
                backgroundColorButton.setBackground(choice);
                setParagraphAttributes(StyleConstants.Background, choice);
            }
        });

        fontSizeChangeListener = e -> {
            selectParagraphElementIfHasNoSelection();
            setCharacterAttributes(StyleConstants.FontSize, fontSizeSpinner.getValue() + PIXEL);
        };
        fontFamilyItemListener = e -> {
            selectParagraphElementIfHasNoSelection();
            setCharacterAttributes(StyleConstants.FontFamily, fontFamilyCombo.getSelectedItem());
        };

        htmlDocumentCaretListener = e -> initByHtmlDocument();
        htmlEditorPane.setEditorKit(htmlEditorKit);
        htmlEditorPane.setDocument(htmlDocument);
        fontFamilyCombo.setModel(createFontComboBoxModel());
        fontSizeSpinner.setModel(new SpinnerNumberModel(8, 5, 120, 1));
        fontSizeSpinner.addChangeListener(fontSizeChangeListener);
        fontFamilyCombo.addItemListener(fontFamilyItemListener);
        saveButton.addActionListener(e -> {
            this.setVisible(false);
            if (saveConsumer != null) {
                saveConsumer.accept(getContentAsHtml());
            }
        });
        cancelButton.addActionListener(e -> this.setVisible(false));
        setContentPane(mainPanel);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(1200, 1000));
        foregroundLabel.setFont(foregroundLabel.getFont().deriveFont(Font.BOLD, 16));
        backgroundLabel.setFont(backgroundLabel.getFont().deriveFont(Font.BOLD, 16));
        boldButton.setBorder(null);
        italicButton.setBorder(null);
        underlineButton.setBorder(null);
        pack();
        htmlEditorPane.addAncestorListener(new AncestorListener() {
            @Override
            public void ancestorAdded(AncestorEvent event) {
                htmlEditorPane.requestFocusInWindow();
                htmlEditorPane.setCaretPosition(1);
            }

            @Override
            public void ancestorRemoved(AncestorEvent event) {

            }

            @Override
            public void ancestorMoved(AncestorEvent event) {

            }
        });
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedIndex() == 1) {
                refreshSourceText();
            } else {
                if (!sourceTextPane.getText().equals(getContentAsHtml())) {
                    htmlEditorPane.setText(sourceTextPane.getText());
                    htmlEditorPane.revalidate();
                    htmlEditorPane.repaint();
                }
            }
        });
    }

    private void refreshSourceText() {
        sourceTextPane.setText(getContentAsHtml());
    }

    private void addHtmlDocumentCaretListener() {
        htmlEditorPane.addCaretListener(htmlDocumentCaretListener);
    }

    @Nullable
    private static String colorToHex(@Nullable Color color) {
        if (color==null) return null;
        return "#" + Integer.toHexString(new Color(color.getRed(), color.getGreen(), color.getBlue()).getRGB()).substring(2);
    }

    public static void main(String[] args) {
        HtmlEditor htmlEditor = new HtmlEditor();
        htmlEditor.setSourceHtml("<html>\n"
                + "<body style='width:400p;'>\n"
                + "<h1 style='background:red'>Welcome!</h1>\n"
                + "<h2>This is an H2 header</h2>\n"
                + "<p style='text-align:right;font-size:55px;'>This is some sample text</p>\n"
                + "<p><a href=\"http://devdaily.com/blog/\">devdaily blog</a></p>\n"
                + "</body>\n");
        htmlEditor.setVisible(true);
        htmlEditor.setSaveConsumer(System.out::println);
    }

    private static RuntimeException createRuntimeException(Exception e) {
        return new RuntimeException(e.getMessage(), e);
    }

    public void setSourceHtml(String html) {
        removeHtmlDocumentCaretListener();
        htmlEditorPane.setText(html);
        sourceTextPane.setText(html);
        addHtmlDocumentCaretListener();
    }

    private void removeHtmlDocumentCaretListener() {
        htmlEditorPane.removeCaretListener(htmlDocumentCaretListener);
    }

    private void initByHtmlDocument() {
        Integer fontSize = (Integer) getParagraphElementStyle(StyleConstants.FontSize);
        String fontFamily = (String) getParagraphElementStyle(StyleConstants.FontFamily);
        Object fontSizeText = getParagraphElementStyle(CSS.Attribute.FONT_SIZE);
        Color foreground = (Color) getParagraphElementStyle(StyleConstants.Foreground);
        Color background = (Color) getParagraphElementStyle(StyleConstants.Background);
        boolean bold = getParagraphElementStyle(StyleConstants.FontConstants.Bold) == Boolean.TRUE;
        boolean italic = getParagraphElementStyle(StyleConstants.Italic) == Boolean.TRUE;
        boolean underline = getParagraphElementStyle(StyleConstants.Underline) == Boolean.TRUE;

        if (fontSizeText != null && fontSizeText.toString().contains(PIXEL)) {
            fontSize = Integer.parseInt(fontSizeText.toString().replaceAll(PIXEL, ""));
        }

        fontSizeSpinner.removeChangeListener(fontSizeChangeListener);
        fontSizeSpinner.setValue(fontSize);
        fontSizeSpinner.addChangeListener(fontSizeChangeListener);
        fontFamilyCombo.removeItemListener(fontFamilyItemListener);
        fontFamilyCombo.setSelectedItem(getFontFamilyItem(fontFamily));
        fontFamilyCombo.addItemListener(fontFamilyItemListener);

        if (foreground == null) {
            foreground = Color.BLACK;
        }

        if (background == null) {
            background = Color.WHITE;
        }

        foregroundColorButton.setForeground(foreground);
        foregroundColorButton.setBackground(foreground);
        backgroundColorButton.setForeground(background);
        backgroundColorButton.setBackground(background);

        boldButton.setBorder(bold ? BorderFactory.createLineBorder(Color.BLACK, 2): null);
        italicButton.setBorder(italic ? BorderFactory.createLineBorder(Color.BLACK, 2): null);
        underlineButton.setBorder(underline ? BorderFactory.createLineBorder(Color.BLACK, 2): null);
    }

    private String getFontFamilyItem(String fontFamily) {
        for (int i = 0; i < fontFamilyCombo.getModel().getSize(); i++) {
            String font = fontFamilyCombo.getModel().getElementAt(i);
            if (font.equalsIgnoreCase(fontFamily)) {
                return font;
            }
        }

        return null;
    }

    private Object getParagraphElementStyle(Object styleName) {
        if (hasNoSelection()) {
            Element characterElement = htmlDocument.getCharacterElement(htmlEditorPane.getCaretPosition());
            Object values = characterElement.getAttributes().getAttribute(styleName);
            if (values != null) return values;
        } else {
            Object values = getCharacterElementStyle(styleName, htmlEditorPane.getSelectionStart(), htmlEditorPane.getSelectionEnd());
            if (values != null) return values;
        }

        Element paragraphElement = htmlDocument.getParagraphElement(htmlEditorPane.getCaretPosition());

        if (paragraphElement.getAttributes().getAttribute(styleName) != null) {
            return paragraphElement.getAttributes().getAttribute(styleName);
        }

        StyleSheet styleSheet = htmlEditorKit.getStyleSheet();
        Style elementStyle = styleSheet.getStyle(paragraphElement.getName());

        if (elementStyle != null && elementStyle.getAttribute(styleName) != null) {
            return elementStyle.getAttribute(styleName);
        }

        if (htmlDocument.getStyle(StyleContext.DEFAULT_STYLE).getAttribute(styleName) != null) {
            return htmlDocument.getStyle(StyleContext.DEFAULT_STYLE).getAttribute(styleName);
        }

        elementStyle = styleSheet.getStyle("body");

        return elementStyle.getAttribute(styleName);
    }

    private Object getCharacterElementStyle(Object styleName, int startOffset, int endOffset) {
        Set<Object> values = new HashSet<>();

        for (int i = startOffset; i < endOffset; i++) {
            Object value = htmlDocument.getCharacterElement(i).getAttributes().getAttribute(styleName);
            if (value != null) {
                values.add(value);
            }
        }

        if (!values.isEmpty()) {
            return values.stream().findFirst().get();
        }

        return null;
    }

    private void setSelectedTextCharacterAttributes(Object styleName, IntPredicate match) {
        selectParagraphElementIfHasNoSelection();
        setCharacterAttributes(styleName, !getSelectionRange().allMatch(match));
        initByHtmlDocument();
    }

    private void setCharacterAttributes(AttributeSet attribute) {
        int selectionStart = htmlEditorPane.getSelectionStart();
        int selectionEnd = htmlEditorPane.getSelectionEnd();
        htmlDocument.setCharacterAttributes(selectionStart, selectionEnd - selectionStart, attribute, false);
    }

    private void selectParagraphElementIfHasNoSelection() {
        if (hasNoSelection()) {
            Element paragraphElement = htmlDocument.getParagraphElement(htmlEditorPane.getCaretPosition());
            htmlEditorPane.setSelectionStart(paragraphElement.getStartOffset());
            htmlEditorPane.setSelectionEnd(paragraphElement.getEndOffset() - 1);
        }
    }

    private boolean hasNoSelection() {
        return htmlEditorPane.getSelectionStart() == htmlEditorPane.getSelectionEnd();
    }

    private void setCharacterAttributes(Object styleName, Object value) {
        AttributeSet attribute = StyleContext.getDefaultStyleContext().addAttribute(SimpleAttributeSet.EMPTY, styleName, value);
        int selectionStart = htmlEditorPane.getSelectionStart();
        int selectionEnd = htmlEditorPane.getSelectionEnd();
        htmlDocument.setCharacterAttributes(selectionStart, selectionEnd - selectionStart, attribute, false);
    }

    private void setParagraphAttributes(Object styleName, Object value) {
        AttributeSet attribute = StyleContext.getDefaultStyleContext().addAttribute(SimpleAttributeSet.EMPTY, styleName, value);
        setParagraphAttributes(attribute);
        initByHtmlDocument();
    }

    private void setParagraphAttributes(AttributeSet attribute) {
        Element paragraphElement = htmlDocument.getParagraphElement(htmlEditorPane.getCaretPosition());
        htmlEditorPane.setSelectionStart(paragraphElement.getStartOffset());
        htmlEditorPane.setSelectionEnd(paragraphElement.getEndOffset() - 1);
        htmlDocument.setParagraphAttributes(htmlEditorPane.getSelectionStart(), htmlEditorPane.getSelectionEnd() - htmlEditorPane.getSelectionStart(), attribute, false);
        htmlDocument.setCharacterAttributes(htmlEditorPane.getSelectionStart(), htmlEditorPane.getSelectionEnd() - htmlEditorPane.getSelectionStart(), attribute, false);
    }

    private IntStream getSelectionRange() {
        return IntStream.range(htmlEditorPane.getSelectionStart(), htmlEditorPane.getSelectionEnd());
    }

    private boolean isBold(int pos) {
        return htmlDocument.getCharacterElement(pos).getAttributes().getAttribute(StyleConstants.FontConstants.Bold) == Boolean.TRUE;
    }

    private boolean isItalic(int pos) {
        return htmlDocument.getCharacterElement(pos).getAttributes().getAttribute(StyleConstants.FontConstants.Italic) == Boolean.TRUE;
    }

    private boolean isUnderline(int pos) {
        return htmlDocument.getCharacterElement(pos).getAttributes().getAttribute(StyleConstants.FontConstants.Underline) == Boolean.TRUE;
    }

    private String getContentAsHtml() {
        return htmlEditorPane.getText();
    }

    private DefaultComboBoxModel<String> createFontComboBoxModel() {
        return new DefaultComboBoxModel<>(Arrays.stream(GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts())
                                                .map(Font::getFamily)
                                                .distinct()
                                                .toArray(String[]::new));
    }

    public Consumer<String> getSaveConsumer() {
        return saveConsumer;
    }

    public void setSaveConsumer(Consumer<String> saveConsumer) {
        this.saveConsumer = saveConsumer;
    }
}
