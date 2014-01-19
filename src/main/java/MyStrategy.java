import model.*;

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




        startMatch();
        doMove();
    }

    void startMatch() {
        //TODO задание начальной конфигурации, создание массивов, списков, коллекций и т. п.
    }

    void doMove() {
        //TODO перебор ходов и выбор наилучшего исходя из заданной статистики
    }
}