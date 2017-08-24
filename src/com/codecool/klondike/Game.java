package com.codecool.klondike;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;

import java.util.*;

import static com.codecool.klondike.Pile.PileType.FOUNDATION;
import static com.codecool.klondike.Pile.PileType.TABLEAU;

public class Game extends Pane {

    private List<Card> deck = new ArrayList<>();

    private Pile stockPile;
    private Pile discardPile;
    private List<Pile> foundationPiles = FXCollections.observableArrayList();
    private List<Pile> tableauPiles = FXCollections.observableArrayList();
    private List<Pile> allPiles = FXCollections.observableArrayList();

    private double dragStartX, dragStartY;
    private List<Card> draggedCards = FXCollections.observableArrayList();


    private static double STOCK_GAP = 1;
    private static double FOUNDATION_GAP = 0;
    private static double TABLEAU_GAP = 30;

    private EventHandler<MouseEvent> onMouseClickedHandler = e -> {
        Card card = (Card) e.getSource();
        if (card.getContainingPile().getPileType() == Pile.PileType.STOCK) {
            card.moveToPile(discardPile);
            card.flip();
            card.setMouseTransparent(false);
            System.out.println("Placed " + card + " to the waste.");
        }
    };

    private EventHandler<MouseEvent> stockReverseCardsHandler = e -> {
        refillStockFromDiscard();
    };

    private EventHandler<MouseEvent> onMousePressedHandler = e -> {
        dragStartX = e.getSceneX();
        dragStartY = e.getSceneY();
    };

    private EventHandler<MouseEvent> onMouseDraggedHandler = e -> {
        Card card = (Card) e.getSource();
        Pile activePile = card.getContainingPile();
        List<Card> cards = activePile.getCards();
        if (activePile.getPileType() == Pile.PileType.STOCK)
            return;
        double offsetX = e.getSceneX() - dragStartX;
        double offsetY = e.getSceneY() - dragStartY;

        draggedCards.clear();

        if (!card.isFaceDown()) {
            draggedCards.add(card);
            card.getDropShadow().setRadius(20);
            card.getDropShadow().setOffsetX(10);
            card.getDropShadow().setOffsetY(10);

            card.toFront();
            card.setTranslateX(offsetX);
            card.setTranslateY(offsetY);

            if (activePile.getPileType() == Pile.PileType.TABLEAU) {
                for (Card c : cards) {
                    if (card.getRank() > c.getRank() && !c.isFaceDown()) {
                        draggedCards.add(c);
                        c.getDropShadow().setRadius(20);
                        c.getDropShadow().setOffsetX(10);
                        c.getDropShadow().setOffsetY(10);
                        c.toFront();
                        c.setTranslateX(offsetX);
                        c.setTranslateY(offsetY);
                    }
                }
            }
        }
    };

    private EventHandler<MouseEvent> onMouseReleasedHandler = e -> {
        if (draggedCards.isEmpty())
            return;
        Card card = (Card) e.getSource();
        Pile pile = getValidIntersectingPile(card, foundationPiles);
        Pile pile2 = getValidIntersectingPile(card, tableauPiles);
        if (pile != null) {
            handleValidMove(card, pile);
            if(isGameWon()){
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle(null);
                alert.setContentText("Congrats, you won :)");
                alert.setHeaderText(null);
                alert.showAndWait();
            }
        }
        else if (pile2 != null){
            handleValidMove(card, pile2);
        }
        else {
            draggedCards.forEach(MouseUtil::slideBack);
            draggedCards.clear();
        }
    };

    public boolean isGameWon() {
        int numberOfTableauCards = 0;
        for(Pile pile: tableauPiles){
            numberOfTableauCards += pile.numOfCards();
        }
        if(stockPile.numOfCards() + numberOfTableauCards + discardPile.numOfCards() == 1){
            return true;
        }
        return false;
    }

    public Game() {
        deck = Card.createNewDeck();
        initPiles();
        dealCards();
    }

    public void addMouseEventHandlers(Card card) {
        card.setOnMousePressed(onMousePressedHandler);
        card.setOnMouseDragged(onMouseDraggedHandler);
        card.setOnMouseReleased(onMouseReleasedHandler);
        card.setOnMouseClicked(onMouseClickedHandler);
    }

    public void refillStockFromDiscard() {
        List<Card> cards = discardPile.getCards();
        List<Card> tempList = new ArrayList<>();
        Iterator<Card> discardCard = cards.iterator();

        for (int i = 0; i < cards.size(); i++) {
            Card card = discardCard.next();
            tempList.add(card);
        }

        discardPile.getCards().clear();
        Collections.reverse(tempList);

        for (Card card : tempList) {
            card.flip();
            stockPile.addCard(card);
        }


        System.out.println("Stock refilled from discard pile.");
    }

    public boolean isMoveValid(Card card, Pile destPile) {
        //TODO
        if (destPile.getTopCard() != null) {
            if (destPile.getPileType() == FOUNDATION && (destPile.getTopCard().getRank() + 1) == card.getRank() &&
                    Card.isSameSuit(destPile.getTopCard(), card)) {
                return true;
            } else if (destPile.getPileType() == TABLEAU && (destPile.getTopCard().getRank()-1) == card.getRank() &&
                    Card.isOppositeColor(destPile.getTopCard(), card)) {
                return true;
            }
        } else if (destPile.getTopCard() == null) {
            if (destPile.getPileType() == FOUNDATION && card.getRank() == 1) {
                return true;
            } else if (destPile.getPileType() == TABLEAU && card.getRank() == 13) {
                return true;
            }
        }
        return false;
    }

    private Pile getValidIntersectingPile(Card card, List<Pile> piles) {
        Pile result = null;
        for (Pile pile : piles) {
            if (!pile.equals(card.getContainingPile()) &&
                    isOverPile(card, pile) &&
                    isMoveValid(card, pile))
                result = pile;
        }
        return result;
    }

    private boolean isOverPile(Card card, Pile pile) {
        if (pile.isEmpty())
            return card.getBoundsInParent().intersects(pile.getBoundsInParent());
        else
            return card.getBoundsInParent().intersects(pile.getTopCard().getBoundsInParent());
    }

    private void handleValidMove(Card card, Pile destPile) {
        String msg = null;
        if (destPile.isEmpty()) {
            if (destPile.getPileType().equals(FOUNDATION))
                msg = String.format("Placed %s to the foundation.", card);
            if (destPile.getPileType().equals(Pile.PileType.TABLEAU))
                msg = String.format("Placed %s to a new pile.", card);

        } else {
            msg = String.format("Placed %s to %s.", card, destPile.getTopCard());
        }
        System.out.println(msg);
        MouseUtil.slideToDest(draggedCards, destPile);
        draggedCards.clear();
    }


    private void initPiles() {
        stockPile = new Pile(Pile.PileType.STOCK, "Stock", STOCK_GAP);
        stockPile.setBlurredBackground();
        stockPile.setLayoutX(95);
        stockPile.setLayoutY(20);
        stockPile.setOnMouseClicked(stockReverseCardsHandler);
        getChildren().add(stockPile);

        discardPile = new Pile(Pile.PileType.DISCARD, "Discard", STOCK_GAP);
        discardPile.setBlurredBackground();
        discardPile.setLayoutX(285);
        discardPile.setLayoutY(20);
        getChildren().add(discardPile);

        for (int i = 0; i < 4; i++) {
            Pile foundationPile = new Pile(FOUNDATION, "Foundadtion " + i, FOUNDATION_GAP);
            foundationPile.setBlurredBackground();
            foundationPile.setLayoutX(610 + i * 180);
            foundationPile.setLayoutY(20);
            foundationPiles.add(foundationPile);
            allPiles.add(foundationPile);
            getChildren().add(foundationPile);
        }
        for (int i = 0; i < 7; i++) {
            Pile tableauPile = new Pile(Pile.PileType.TABLEAU, "Tableau " + i, TABLEAU_GAP);
            tableauPile.setBlurredBackground();
            tableauPile.setLayoutX(95 + i * 180);
            tableauPile.setLayoutY(275);
            tableauPiles.add(tableauPile);
            allPiles.add(tableauPile);
            getChildren().add(tableauPile);
        }
    }


    public void dealCards() {
        Iterator<Card> deckIterator = deck.iterator();

        for (int i = 0; i < 7; i++) {
            for (int j = 0; j < (i + 1); j++) {
                Card card = deckIterator.next();
                tableauPiles.get(i).addCard(card);
                deckIterator.remove();
                addMouseEventHandlers(card);
                getChildren().add(card);
            }
            tableauPiles.get(i).getTopCard().flip();
        }

        deckIterator.forEachRemaining(card -> {
            stockPile.addCard(card);
            addMouseEventHandlers(card);
            getChildren().add(card);
        });
    }

    public void setTableBackground(Image tableBackground) {
        setBackground(new Background(new BackgroundImage(tableBackground,
                BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT,
                BackgroundPosition.CENTER, BackgroundSize.DEFAULT)));
    }


    public List<Pile> getTableauPiles() {
        return tableauPiles;
    }

}
