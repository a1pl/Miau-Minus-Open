package miau.ui.clickgui.components;

public interface FocusableTextComponent {
    boolean isTextInputFocused();

    void unfocusTextInput();

    boolean containsClick(int var1, int var2);
}
