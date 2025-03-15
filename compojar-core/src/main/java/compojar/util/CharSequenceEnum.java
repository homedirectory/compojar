package compojar.util;

public interface CharSequenceEnum extends CharSequence {

    @Override
    default int length() {
        return name().length();
    }

    @Override
    default char charAt(int index) {
        return name().charAt(index);
    }

    @Override
    default CharSequence subSequence(int start, int end) {
        return name().subSequence(start, end);
    }

    String name();

}
