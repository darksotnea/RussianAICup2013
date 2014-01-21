import model.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class MyStrategy implements Strategy {
    Trooper self;
    World world;
    Game game;
    Move move;
    Trooper[] troopers;
    Player[] players;
    Bonus[] bonuses;
    CellType[][] cells;
    int H;
    int W;
    Trooper commander;
    Trooper medic;
    Trooper soldier;
    Trooper sniper;
    Trooper scout;

    @Override
    public void move(Trooper self, World world, Game game, Move move) {
        this.self = self;
        this.world = world;
        this.move = move;
        this.game = game;
        this.troopers = world.getTroopers();
        this.bonuses = world.getBonuses();
        this.players = world.getPlayers();
        this.cells = world.getCells();
        H = world.getHeight();
        W = world.getWidth();

        initMatch();
        calculateMove();
        finishMatch();

    }

    void initMatch() {
        //TODO задание начальной конфигурации, создание массивов, списков, коллекций и т. п.

    }

    void calculateMove() {
        //TODO перебор ходов и выбор наилучшего исходя из заданной статистики
    }

    void finishMatch() {
        //TODO конечные вычисления
    }

    public ArrayList<Trooper> getTeammates() {
        commander = null;
        medic = null;
        soldier = null;
        sniper = null;
        scout = null;

        ArrayList<Trooper> r = new ArrayList<>();
        for (Trooper trooper : world.getTroopers()) {
            if (trooper.isTeammate()) {
                r.add(trooper);
                switch (trooper.getType()) {
                    case COMMANDER:
                        commander = trooper;
                        break;
                    case FIELD_MEDIC:
                        medic = trooper;
                        break;
                    case SOLDIER:
                        soldier = trooper;
                        break;
                    case SNIPER:
                        sniper = trooper;
                        break;
                    case SCOUT:
                        scout = trooper;
                        break;
                }
            }
        }
        Collections.sort(r, new Comparator<Trooper>() {
            @Override
            public int compare(Trooper o1, Trooper o2) {
                return Long.compare(o1.getId(), o2.getId());
            }
        });
        return r;
    }
}