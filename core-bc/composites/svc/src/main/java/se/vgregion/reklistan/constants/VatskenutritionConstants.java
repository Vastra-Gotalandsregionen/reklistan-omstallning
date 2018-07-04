package se.vgregion.reklistan.constants;

/**
 * @author Erik Andersson - Monator Technologies AB
 */

public interface VatskenutritionConstants {

    public static final String READ_ROLE_NAME = "Läsa artiklar";
    public static final String WRITE_ROLE_NAME = "Skriva artiklar";
    public static final String PUBLISH_ROLE_NAME = "Publicera artiklar";

    public static final String[] JOURNAL_ARTICLE_ACTIONS_ALL = new String[]{"ADD_DISCUSSION", "DELETE", "DELETE_DISCUSSION", "EXPIRE", "PERMISSIONS", "UPDATE", "UPDATE_DISCUSSION", "VIEW"};
    public static final String[] JOURNAL_ARTICLE_ACTIONS_VIEW = new String[]{"VIEW"};
    public static final String[] JOURNAL_ARTICLE_ACTIONS_UPDATE = new String[]{"UPDATE"};
    public static final String[] JOURNAL_ARTICLE_ACTIONS_VIEW_UPDATE = new String[]{"UPDATE"};


}