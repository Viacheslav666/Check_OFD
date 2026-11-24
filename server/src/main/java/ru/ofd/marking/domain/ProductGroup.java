package ru.ofd.marking.domain;

public enum ProductGroup {
    BAKALEYA("Бакалея"),
    TOBACCO("Табак"),
    NICOTINE("Никотинсодержащая"),
    BEER("Пиво"),
    DRAFT_BEER("Разливное пиво"),
    WATER("Вода"),
    DAIRY("Молочка"),
    JUICE("Соки"),
    OTHER("Прочее");

    private final String titleRu;

    ProductGroup(String titleRu) {
        this.titleRu = titleRu;
    }

    public String getTitleRu() {
        return titleRu;
    }
}
