import model.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Random;

import static java.lang.StrictMath.hypot;

public final class MyStrategy implements Strategy {
    private final int HP_WHEN_HEAL = 99;
    private final int HP_WHEN_GO_MEDIC = 96;
    private final int AREA_OF_COMMANDER = 4;
    private final int AREA_OF_SOLDIER = 5;
    private final int AREA_OF_SNIPER = 5;
    private final int AREA_OF_SCOUT = 4;
    private final int AREA_OF_GRENADE = 5;
    private final int ACTION_POINT_OF_GRENADE_THROW = 8;
    private final int ACTION_POINT_OF_MEDIKIT_USE = 2;
    private final int ACTION_POINT_OF_FIELD_RATION_EAT = 2;
    private final int MIN_DISTANCE_FOR_LOCALTARGET = 1;
    private final int TIME_EXPIRE_OF_LISTOFSOWENEMYS = 2;
    private static boolean beginBattle = false;
    private static thePoint safePoint;
    private static boolean goToSafePlace = false;
    private static int globalTargetX = -1;
    private static int globalTargetY = -1;
    private static int lastMoveX = 0;
    private static int lastMoveY = 0;
    private static int localTargetX = 100;
    private static int localTargetY = 100;
    private static boolean isDetectANextTrooper = false;
    private static boolean isAfterExplore = false;
    private static boolean detectEnemyByTeam = false;
    private static boolean getHelpFromAir = false;
    private static boolean needHelpFromAir = false;
    private static boolean istroopersUnderAttack = false;
    private static int trooperUnderAttack = -1;
    private static int numOfTroopers = 0;
    private static int teamSupportCount;
    private int indexOfCommander;
    private int indexOfMedic;
    private int indexOfSoldier;
    private int indexOfSniper;
    private int indexOfScout;
    private final Random random = new Random();
    private static boolean isOrder = false;
    private static LinkedList<TrooperType> listOfOrderMovesOfTroopers = new LinkedList<>();
    private static LinkedList<Player> listOfOrderMovesOfPlayers = new LinkedList<>();
    private static Player[] listOfOrderMovesOfPlayersTemp = null;
    private static TrooperType lastTrooperType;
    private static Trooper targetTrooper = null;
    private static LinkedList<Trooper> listOfEnemys;
    private static LinkedList<thePoint> complatedPathOfTrooper = new LinkedList<>();
    private static ArrayList<thePoint> stayOnTailList = new ArrayList<>();
    private static int[][] hpOfTroopers;
    private static LinkedList<Integer> remainingQuarters = new LinkedList<>();
    private static int[][] cellsInt;
    private static int[][] trueMapOfPoints;
    private static ArrayList <ListOfPlayers> listOfPlayers = new ArrayList<>();
    private static ListOfPlayers crushedPlayer;
    Trooper[] troopers;
    Bonus[] bonuses;
    World world;
    Game game;
    Move move;
    private static Trooper targetHeal = null;
    private int teamCount;
    private static int saveMoveWorld = -1;
    private static int saveMoveSafePlace = -1;
    private static int savedTrooperId = -1;
    private static boolean goThrowGrenade = false;
    private static LinkedList<GameUnit> listOfSowEnemys = new LinkedList<>();
    private static LinkedList<Trooper> listOfEnemyTroopers;
    private static boolean isEnemyStrategyCrashed = false;
    private boolean goToBonus = false;
    private static int myScore = 0;
    private static boolean isShootingAnywhere = false;
    private static int targetUnitIdSave = -1;
    private static int idOfTrooperStop = -1;
    private static boolean isThrowGrenadeOnSowTroopers = false;
    private static boolean isUseLastMove = false;
    private TrooperStance safeStance;
    private static int forwardTrooper = -1;
    private Bonus bonusTarget = null;
    private LinkedList<thePoint> listOfStoredCells = new LinkedList<thePoint>();
    private static boolean enemyInAmbush = false;
    private static thePoint lastPositionPointForTrooperIsUnderAttack = null;
    private static boolean isGoNear = false;

    @Override
    public void move(Trooper self, World world, Game game, Move move) {
        this.world = world;
        this.move = move;
        this.game = game;

        // ТАБЛИЦА СТРЕЛЬБЫ
            /*
            Характеристика бойца         Командир    Полевой медик   Штурмовик  Снайпер     Разведчик
            Очки здоровья (нач./макс.)  100/100     100/100         120/100     100/100     100/100
            Очки действия               10          10              10          10          12
            Дальность обзора            8           7               7           7           9
            Дальность стрельбы          7           5               8           10          6
            Урон от выстрела (стоя)     15          9               25          65          20
            Урон от выстрела (сидя)     20          12              30          80          25
            Урон от выстрела (лёжа)     25          15              35          95          30
            Стоимость выстрела          3           2               4           9           4
            Начальный бонус             —       Аптечка         Граната        —        Сухой паёк
            */

        troopers = world.getTroopers();
        bonuses = world.getBonuses();

        //получение списка врагов и обновление параметров юнитов, таких как индексы в массиве troopers и их ХП.
        listOfEnemys = new LinkedList<>();
        teamCount = 0;

        indexOfCommander = -1;
        indexOfMedic = -1;
        indexOfSoldier = -1;
        indexOfSniper = -1;
        indexOfScout = -1;

        for (Trooper trooper : troopers) {
            if (!trooper.isTeammate()) {
                listOfEnemys.add(trooper);
            }
        }

        for (int i = 0; i < troopers.length; i++) {
            if (troopers[i].getType() == TrooperType.FIELD_MEDIC && troopers[i].isTeammate()) {
                indexOfMedic = i;
                teamCount++;
            }

            if (troopers[i].getType() == TrooperType.COMMANDER && troopers[i].isTeammate()) {
                indexOfCommander = i;
                teamCount++;
            }

            if (troopers[i].getType() == TrooperType.SOLDIER && troopers[i].isTeammate()) {
                indexOfSoldier = i;
                teamCount++;
            }

            if (troopers[i].getType() == TrooperType.SNIPER && troopers[i].isTeammate()) {
                indexOfSniper = i;
                teamCount++;
            }

            if (troopers[i].getType() == TrooperType.SCOUT && troopers[i].isTeammate()) {
                indexOfScout = i;
                teamCount++;
            }
        }

        if (indexOfScout != -1) {
            forwardTrooper = indexOfScout;
        } else if (indexOfCommander != -1) {
            forwardTrooper = indexOfCommander;
        } else if (indexOfSoldier != -1) {
            forwardTrooper = indexOfSoldier;
        } else if (indexOfMedic != -1) {
            forwardTrooper = indexOfMedic;
        }

        if (lastTrooperType != self.getType() && teamCount > 1) {

            complatedPathOfTrooper = new LinkedList<>();

            //ошибка с выходом на пределы массива при удалении? добавить цикл do while
            for (thePoint point : listOfStoredCells) {
                if (world.getMoveIndex() - point.worldMove > 1) {
                    listOfStoredCells.remove(point);
                }
            }

            lastMoveX = self.getX();
            lastMoveY = self.getY();

            safePoint = null;
            safeStance = null;
            goToSafePlace = false;
            saveMoveSafePlace = -1;

            isAfterExplore = false;

            savedTrooperId = -1;
            idOfTrooperStop = -1;

            isDetectANextTrooper = true;

            forwardTrooper = -1;

            bonusTarget = null;
            goToBonus = false;

            if(indexOfMedic == -1) {
                targetHeal = null;
            }

            for (Trooper trooper : troopers) {
                boolean flag = false;
                if (lastTrooperType == trooper.getType() && trooper.isTeammate()) {
                    for (int i = 0; i < hpOfTroopers.length; i++) {
                        if (trooper.getId() == hpOfTroopers[i][0]) {
                            if (world.getMoveIndex() == stayOnTailList.get(i).worldMove) {

                                if (stayOnTailList.get(i).getX() == trooper.getX() && stayOnTailList.get(i).getY() == trooper.getY()) {

                                    stayOnTailList.get(i).indexOfTailTime++;
                                    stayOnTailList.get(i).worldMove++;
                                    flag = true;
                                    break;

                                } else {
                                    stayOnTailList.set(i, new thePoint(trooper.getX(), trooper.getY()));
                                    flag = true;
                                    break;
                                }

                            } else if (world.getMoveIndex() != stayOnTailList.get(i).worldMove) {
                                flag = true;
                                break;
                            }
                        }
                    }
                }
                if (flag) {
                    break;
                }
            }

        } else if (teamCount == 1 && (self.getActionPoints() == 10 && self.getType() != TrooperType.SCOUT || self.getActionPoints() == 12)) {

            complatedPathOfTrooper = new LinkedList<>();

            //ошибка с выходом на пределы массива при удалении? добавить цикл do while
            for (thePoint point : listOfStoredCells) {
                if (world.getMoveIndex() - point.worldMove > 1) {
                    listOfStoredCells.remove(point);
                }
            }

            safePoint = null;
            safeStance = null;
            goToSafePlace = false;
            saveMoveSafePlace = -1;

            isAfterExplore = false;

            savedTrooperId = -1;
            idOfTrooperStop = -1;

            isDetectANextTrooper = true;

            forwardTrooper = -1;

            bonusTarget = null;
            goToBonus = false;

            if(indexOfMedic == -1) {
                targetHeal = null;
            }

            for (Trooper trooper : troopers) {
                boolean flag = false;
                if (lastTrooperType == trooper.getType() && trooper.isTeammate()) {
                    for (int i = 0; i < hpOfTroopers.length; i++) {
                        if (trooper.getId() == hpOfTroopers[i][0]) {
                            if (world.getMoveIndex() == stayOnTailList.get(i).worldMove) {

                                if (stayOnTailList.get(i).getX() == trooper.getX() && stayOnTailList.get(i).getY() == trooper.getY()) {

                                    stayOnTailList.get(i).indexOfTailTime++;
                                    stayOnTailList.get(i).worldMove++;
                                    flag = true;
                                    break;

                                } else {
                                    stayOnTailList.set(i, new thePoint(trooper.getX(), trooper.getY()));
                                    flag = true;
                                    break;
                                }

                            } else if (world.getMoveIndex() != stayOnTailList.get(i).worldMove) {
                                flag = true;
                                break;
                            }
                        }
                    }
                }
                if (flag) {
                    break;
                }
            }
        }

        lastTrooperType = self.getType();

        //строим карту с приоритетом ячеек
        trueMapOfPoints = getMapOfPoints(self);

        if (targetTrooper == null) {
            beginBattle = false;
        } else {
            enemyInAmbush = false;
        }

        if (!(saveMoveWorld == world.getMoveIndex() || saveMoveWorld == world.getMoveIndex() - 1)) {
            istroopersUnderAttack = false;
            trooperUnderAttack = -1;
        }

        for (Trooper trooper : troopers) {
            if (trooperUnderAttack == trooper.getId() && !(trooper.getX() == lastPositionPointForTrooperIsUnderAttack.getX() && trooper.getY() == lastPositionPointForTrooperIsUnderAttack.getY())) {
                trooperUnderAttack = -1;
                break;
            }

        }

        if (trooperUnderAttack != -1) {
            boolean test = false;
            for (Trooper trooper : troopers) {
                if (trooperUnderAttack == trooper.getId()) {
                    test = true;
                }
            }
            if (!test) {
                trooperUnderAttack = -1;
            }
        }

        if (goToSafePlace) {
            saveMoveSafePlace = world.getMoveIndex();
            savedTrooperId = (int) self.getId();
        } else if (saveMoveSafePlace == world.getMoveIndex() && savedTrooperId == self.getId()) {
            if (self.getType() == TrooperType.FIELD_MEDIC && goHeal(self)) {
                return;
            }
            move.setAction(ActionType.END_TURN);
            return;
        }

        if (safeStance != null && safeStance == self.getStance() && saveMoveSafePlace == world.getMoveIndex()) {
            if (targetTrooper != null && canShootOnTarget(self, targetTrooper)) {
                shootOnTarget(self, targetTrooper);
                return;
            }
            if (listOfEnemyTroopers.size() != 0) {
                for (Trooper trooper : listOfEnemyTroopers) {
                    if (canShootOnTarget(self, trooper)) {
                        shootOnTarget(self, trooper);
                        return;
                    }
                }
            }
            if (self.getType() == TrooperType.FIELD_MEDIC) {
                if (goHeal(self)) {
                    if (move.getAction() == ActionType.HEAL) {
                        return;
                    }
                }
            }

            if(listOfEnemyTroopers.size() != 0) {
                move.setAction(ActionType.END_TURN);
                return;
            }
        }

        if (isUseLastMove && listOfEnemys.size() == 0) {
            isUseLastMove = false;
            move.setAction(ActionType.END_TURN);
            return;
        } else if (isUseLastMove && listOfEnemys.size() != 0) {
            isUseLastMove = false;
        }

        if (isShootingAnywhere) {
            isShootingAnywhere = false;
            for (Player player : world.getPlayers()) {
                if (player.getName().equalsIgnoreCase("darkstone")) {
                    for (GameUnit unit : listOfSowEnemys) {
                        if (unit.trooper.getId() == targetUnitIdSave && myScore < player.getScore()) {
                            unit.worldMove = world.getMoveIndex();
                            break;
                        } else if (unit.trooper.getId() == targetUnitIdSave) {
                            if (targetTrooper != null && targetTrooper.getId() == targetUnitIdSave) {
                                targetTrooper = null;
                            }
                            listOfSowEnemys.remove(unit);
                            break;
                        }
                    }
                    break;
                }
            }
        }

        if (isThrowGrenadeOnSowTroopers) {
            int myTempScore = -1;
            for (Player player : world.getPlayers()) {
                if (player.getName().equalsIgnoreCase("darkstone")) {
                    myTempScore = player.getScore();
                }
            }
            if (myTempScore > myScore) {
                isThrowGrenadeOnSowTroopers = false;
            }
        }

        if (listOfSowEnemys.size() != 0) {

            for (Trooper trooper : listOfEnemys) {
                boolean notHere = true;

                //проверяем на соответствие списка listOfSow живым враженскию юнитам, если соответствует, то обновляем worldMove
                for (GameUnit gameUnit : listOfSowEnemys) {
                    if (gameUnit.trooper.getId() == trooper.getId() && (gameUnit.trooper.getX() != trooper.getX() || gameUnit.trooper.getY() != trooper.getY())) {
                        gameUnit.trooper = trooper;
                        gameUnit.worldMove = world.getMoveIndex();
                        gameUnit.isNotInFog = true;
                        notHere = false;
                        break;
                    } else if (gameUnit.trooper.getId() == trooper.getId()) {
                        gameUnit.worldMove = world.getMoveIndex();
                        gameUnit.trooper = trooper;
                        gameUnit.isNotInFog = true;
                        notHere = false;
                        break;
                    }
                }

                //если юнита нету в списке listOfSow, то добавляем его туда
                if(notHere) {
                    GameUnit gameUnit = new GameUnit(trooper);
                    listOfSowEnemys.add(gameUnit);
                    gameUnit.isNotInFog = true;
                }
            }

            //если юнит устарел, то удаляем его из списка
            boolean test;
            do {
                test = false;
                for (GameUnit gameUnit : listOfSowEnemys) {
                    if (Math.abs(world.getMoveIndex() - gameUnit.worldMove) > TIME_EXPIRE_OF_LISTOFSOWENEMYS) {
                        listOfSowEnemys.remove(gameUnit);
                        test = true;
                        break;
                    }
                }
            } while (test);

            //проверяем видна ли ячейка в которой был юнит, если ячейка видна и она пустая, то удалить юнита из listOfSow
            do {
                test = false;
                for (GameUnit gameUnit : listOfSowEnemys) {

                    //пропускаем всех юнитов, которые видимы
                    boolean isInListOfEnemys = false;
                    for (Trooper trooper : listOfEnemys) {
                        if (trooper.getId() == gameUnit.trooper.getId()) {
                            isInListOfEnemys = true;
                            break;
                        }
                    }
                    if (isInListOfEnemys) {
                        continue;
                    }

                    //проверяем клетки в которых находятся невидимые юниты (это значит или они за препятствием (присели или легли) или их уже нет в этой клетке)
                    boolean isOldOutdated = true;
                    int count = 0;
                    int countSee = 0;

                    for (Trooper trooper1 : troopers) {

                        //если свой трупер видит вражеского в enemy.getStance(), то проверяем на видимость в стойке PRONE
                        if (trooper1.isTeammate() && world.isVisible(trooper1.getVisionRange(), trooper1.getX(), trooper1.getY(), trooper1.getStance(), gameUnit.trooper.getX(), gameUnit.trooper.getY(), TrooperStance.PRONE)) {
                            isOldOutdated = true;
                            break;
                        } else if ( !(trooper1.isTeammate() && self.getDistanceTo(gameUnit.trooper) <= self.getVisionRange() && !world.isVisible(trooper1.getVisionRange(), trooper1.getX(), trooper1.getY(), trooper1.getStance(), gameUnit.trooper.getX(), gameUnit.trooper.getY(), gameUnit.trooper.getStance())) && trooper1.isTeammate() && !world.isVisible(trooper1.getVisionRange(), trooper1.getX(), trooper1.getY(), trooper1.getStance(), gameUnit.trooper.getX(), gameUnit.trooper.getY(), TrooperStance.PRONE) && self.getVisionRange() >= self.getDistanceTo(gameUnit.trooper)) {
                            countSee++;
                        } else if (trooper1.isTeammate()) {
                            count++;
                        }
                    }

                    if (countSee > 0) {
                        gameUnit.trooper = new Trooper(gameUnit.trooper.getId(), gameUnit.trooper.getX(), gameUnit.trooper.getY(), gameUnit.trooper.getPlayerId(), gameUnit.trooper.getTeammateIndex(), gameUnit.trooper.isTeammate(), gameUnit.trooper.getType(), TrooperStance.PRONE, gameUnit.trooper.getHitpoints(), gameUnit.trooper.getMaximalHitpoints(), gameUnit.trooper.getActionPoints(), gameUnit.trooper.getInitialActionPoints(), gameUnit.trooper.getVisionRange(), gameUnit.trooper.getShootingRange(), gameUnit.trooper.getShootCost(), gameUnit.trooper.getStandingDamage(), gameUnit.trooper.getKneelingDamage(), gameUnit.trooper.getProneDamage(), gameUnit.trooper.getDamage(), gameUnit.trooper.isHoldingGrenade(), gameUnit.trooper.isHoldingMedikit(), gameUnit.trooper.isHoldingFieldRation());
                        isOldOutdated = false;
                        break;
                    }

                    if (isOldOutdated && count != teamCount) {
                        listOfSowEnemys.remove(gameUnit);
                        test = true;
                        break;
                    }
                }
            } while (test);
        } else {
            //если список listOfSow пуст, то добавляем в него всех из listOfEnemys
            if(listOfEnemys.size() != 0) {
                for (Trooper trooper : listOfEnemys) {
                    GameUnit gameUnit = new GameUnit(trooper);
                    listOfSowEnemys.add(gameUnit);
                    gameUnit.isNotInFog = true;
                }
            }
        }

        listOfEnemyTroopers = new LinkedList<>();

        //заполняем список сначала видимыми, а потом не видимыми врагами, при этом смотрим чтобы они не повторялись
        if (listOfEnemys.size() != 0) {
            for (Trooper trooper : listOfEnemys) {
                boolean test = true;
                for (Trooper trooper1 : listOfEnemyTroopers) {
                    if (trooper.getX() == trooper1.getX() && trooper.getY() == trooper1.getY()) {
                        test = false;
                        break;
                    }
                }
                if (test) {
                    listOfEnemyTroopers.add(trooper);
                }
            }
        }

        if (listOfSowEnemys.size() != 0) {
            for (GameUnit gameUnit : listOfSowEnemys) {
                boolean test = true;
                gameUnit.isNotInFog = false;
                for (Trooper trooper : listOfEnemyTroopers) {
                    if (gameUnit.trooper.getX() == trooper.getX() && gameUnit.trooper.getY() == trooper.getY()) {
                        test = false;
                        gameUnit.isNotInFog = true;
                        break;
                    }
                }
                if (test) {
                    listOfEnemyTroopers.add(gameUnit.trooper);
                }
            }
        }

        if(targetTrooper == null) {
            targetTrooper = chooseEnemyOnDistance(self, listOfEnemyTroopers);
        }

        //Рандомизация обхода карты, оринентирована по углам.
        makeATarget(self);

        //расчёт поддержки при атаке targetTrooper-a
        teamSupportCount = 0;
        for (Trooper trooper : troopers) {
            if (trooper.isTeammate() && trooper.getType() != self.getType()) {
                if (targetTrooper != null && canSeeOrCanShoot(trooper, targetTrooper, false /*&& troopers[indexOfMedic].getDistanceTo(targetTrooper) <= self.getDistanceTo(targetTrooper)*/)) {
                    teamSupportCount++;
                }
            }
        }

        whenHpOfTrooperIsChanged(self);

        if (self.getActionPoints() >= 10) {

            boolean tempValue = hpIsChanged(self);
            if(tempValue) {
                saveMoveWorld = world.getMoveIndex();
                istroopersUnderAttack = tempValue;
            }

            processingHpOfTroopers();
        }

        if (self.getId() == idOfTrooperStop && saveMoveSafePlace == world.getMoveIndex()) {

            boolean isEnd = true;

            for (Trooper trooper : listOfEnemyTroopers) {
                if(world.isVisible(trooper.getVisionRange(), trooper.getX(), trooper.getY(), TrooperStance.STANDING, self.getX(), self.getY(), self.getStance())) {
                    isEnd = false;
                }
            }

            if (isEnd) {
                for (Trooper trooper : listOfEnemyTroopers) {
                    if(world.isVisible(trooper.getVisionRange(), trooper.getX(), trooper.getY(), TrooperStance.STANDING, self.getX(), self.getY(), self.getStance())) {

                    }
                    if (canShootOnTarget(self, trooper)) {
                        move.setAction(ActionType.SHOOT);
                        move.setX(trooper.getX());
                        move.setY(trooper.getY());
                        return;
                    }
                }

                move.setAction(ActionType.END_TURN);
                return;
            }
        }

        orderMove(self);

        //если долго стоим на месте, то значит тупиковая ситуация, выходим из неё при помощи conductTheWar
        if (isDetectANextTrooper) {
            for (Trooper trooper : troopers) {
                boolean flag = false;
                if (self.getType() == trooper.getType() && trooper.isTeammate()) {
                    for (int i = 0; i < hpOfTroopers.length; i++) {
                        if (trooper.getId() == hpOfTroopers[i][0]) {
                            if (stayOnTailList.get(i).getX() == self.getX() && stayOnTailList.get(i).getY() == self.getY()) {

                                if (stayOnTailList.get(i).indexOfTailTime >= 5) {

                                    int myTempScore = -1;
                                    for (Player player : world.getPlayers()) {
                                        if (player.getName().equalsIgnoreCase("darkstone")) {
                                            myTempScore = player.getScore();
                                        }
                                    }

                                    for (Player player : world.getPlayers()) {
                                        if (!player.getName().equalsIgnoreCase("darkstone")) {
                                            if (myTempScore < player.getScore()) {
                                                if (conductTheWar(self)) {
                                                    return;
                                                }
                                            }
                                        }
                                    }
                                }
                                isDetectANextTrooper = false;
                                flag = true;
                                break;
                            } else {
                                flag = true;
                                break;
                            }
                        }
                    }
                }
                if (flag) {
                    break;
                }
            }
        }

        if (isAfterExplore && safePoint == null) {
            if (complatedPathOfTrooper != null && complatedPathOfTrooper.size() > 1 && goOnPath(self, complatedPathOfTrooper.get(complatedPathOfTrooper.size() - 1).getX(), complatedPathOfTrooper.get(complatedPathOfTrooper.size() - 1).getY(), false)) {
                isAfterExplore = false;
                return;
            }
        }

        //   @@@@@@@@@@@@@@@@@@@@@@@         КОМАНДОР             @@@@@@@@@@@@@@@@@@@@@@@@@


        if (self.getType() == TrooperType.COMMANDER) {

            underAttack(self);

            //разведка с воздуха
            if (needHelpFromAir && targetTrooper == null && localTargetX == 100 || world.getMoveIndex() % 10 == 0 && targetTrooper == null && localTargetX == 100 && world.getMoveIndex() != 0) {
                needHelpFromAir = true;
                if (self.getActionPoints() >= game.getCommanderRequestEnemyDispositionCost()) {
                    move.setAction(ActionType.REQUEST_ENEMY_DISPOSITION);
                    getHelpFromAir = true;
                    return;
                }
            }

            if (targetTrooper != null) {

                if (self.getActionPoints() <= 7 && self.isHoldingFieldRation() /*&& !self.isHoldingGrenade()*/ && canSeeOrCanShoot(self, targetTrooper, false)) {
                    useFieldRation(self);
                    return;
                }

                //пытается убить любую вражескую цель, если она убиваема
                if (killAnyEnemyUnit(self)) {
                    return;
                }

                //идёт убивать любую цель, если к ней можно подойти и убить
                if (goAndKillTarget(self)) {
                    return;
                }

                if (makeABoom(self)) {
                    return;
                }

                if (tryToUseMedkit(self) && self.getStance() != TrooperStance.PRONE) {
                    return;
                }

                if (self.getHitpoints() <= 65 && goToMedic(self)) {
                    return;
                }

                if (beginBattle) {
                    if (conductTheWar(self)) {
                        return;
                    }
                }

                if (goOnWar(self, targetTrooper.getX(), targetTrooper.getY())) {
                    return;
                }

                if (goToMedic(self)) {
                    return;
                }

                if (self.getActionPoints() < self.getShootCost()) {
                    if (makeValidLowerStance(self, false)) {
                        return;
                    }
                    move.setAction(ActionType.END_TURN);
                    return;
                }

            } else {

                if (throwGrenadeInMirage(self)) {
                    return;
                }

                if (tryToUseMedkit(self)) {
                    return;
                }

                if (goToMedic(self)) {
                    return;
                }

                if (!(istroopersUnderAttack && trooperUnderAttack == self.getId()) && indexOfMedic != -1 && self.getHitpoints() < HP_WHEN_HEAL && isDistanceEqualOrLessOneTail(self, troopers[indexOfMedic])) {
                    if (self.getActionPoints() >= 2) {
                        if (self.getStance() != TrooperStance.PRONE) {
                            move.setAction(ActionType.LOWER_STANCE);
                            return;
                        }
                    }
                    move.setAction(ActionType.END_TURN);
                    return;
                }
            }
        }


        //   @@@@@@@@@@@@@@@@@@@@@@@         СОЛДАТ             @@@@@@@@@@@@@@@@@@@@@@@@@

        if (self.getType() == TrooperType.SOLDIER) {

            underAttack(self);

            if (targetTrooper != null) {

                if (self.getActionPoints() <= 7 && self.getActionPoints() % 2 == 1 && self.isHoldingFieldRation() && canSeeOrCanShoot(self, targetTrooper, false)) {
                    useFieldRation(self);
                    return;
                }

                if (makeABoom(self)) {
                    return;
                }

                //пытается убить любую вражескую цель, если она убиваема
                if (killAnyEnemyUnit(self)) {
                    return;
                }

                //идёт убивать любую цель, если к ней можно подойти и убить
                if (goAndKillTarget(self)) {
                    return;
                }

                if (tryToUseMedkit(self) && self.getStance() != TrooperStance.PRONE) {
                    return;
                }

                if (self.getHitpoints() <= 65 && goToMedic(self)) {
                    return;
                }

                if (beginBattle) {
                    if (conductTheWar(self)) {
                        return;
                    }
                }

                if (goOnWar(self, targetTrooper.getX(), targetTrooper.getY())) {
                    return;
                }

                if (goToMedic(self)) {
                    return;
                }

                if (self.getActionPoints() < self.getShootCost()) {
                    if (makeValidLowerStance(self, false)) {
                        return;
                    }
                    move.setAction(ActionType.END_TURN);
                    return;
                }

            } else {

                if (throwGrenadeInMirage(self)) {
                    return;
                }

                if (tryToUseMedkit(self)) {
                    return;
                }

                if (goToMedic(self)) {
                    return;
                }

                if (!(istroopersUnderAttack && trooperUnderAttack == self.getId()) && indexOfMedic != -1 && self.getHitpoints() < HP_WHEN_HEAL && isDistanceEqualOrLessOneTail(self, troopers[indexOfMedic])) {
                    if (self.getActionPoints() >= 2) {
                        if (self.getStance() != TrooperStance.PRONE) {
                            move.setAction(ActionType.LOWER_STANCE);
                            return;
                        }
                    }
                    move.setAction(ActionType.END_TURN);
                    return;
                }
            }
        }


        //   @@@@@@@@@@@@@@@@@@@@@@@         МЕДИК             @@@@@@@@@@@@@@@@@@@@@@@@@@@@

        if (self.getType() == TrooperType.FIELD_MEDIC) {

            underAttack(self);

            if (targetTrooper != null) {

                if (self.getActionPoints() <= 7 && self.isHoldingFieldRation() && canSeeOrCanShoot(self, targetTrooper, false)) {
                    useFieldRation(self);
                    return;
                }

                if (makeABoom(self)) {
                    return;
                }

                //пытается убить любую вражескую цель, если она убиваема
                if (killAnyEnemyUnit(self)) {
                    return;
                }

                //идёт убивать любую цель, если к ней можно подойти и убить
                if (goAndKillTarget(self)) {
                    return;
                }

                if (self.getActionPoints() == 2) {
                    if (tryToUseMedkit(self)) {
                        return;
                    }
                }

                if (goHeal(self)) {
                    return;
                }

                if (beginBattle) {
                    if (conductTheWar(self)) {
                        return;
                    }
                }

                if (goOnWar(self, targetTrooper.getX(), targetTrooper.getY())) {
                    return;
                }

                if (self.getActionPoints() < self.getShootCost()) {
                    if (makeValidLowerStance(self, false)) {
                        return;
                    }
                    move.setAction(ActionType.END_TURN);
                    return;
                }
            } else {

                if (throwGrenadeInMirage(self)) {
                    return;
                }

                if (!(istroopersUnderAttack && trooperUnderAttack == self.getId()) && goHeal(self) && !goToSafePlace) {
                    return;
                }

                if (tryToUseMedkit(self)) {
                    return;
                }
            }
        }


        //   @@@@@@@@@@@@@@@@@@@@@@@        РАЗВЕДЧИК             @@@@@@@@@@@@@@@@@@@@@@@@@@@@
        if (self.getType() == TrooperType.SCOUT) {

            underAttack(self);

            if (targetTrooper != null) {

                if (self.getActionPoints() <= 7 && self.isHoldingFieldRation() && canSeeOrCanShoot(self, targetTrooper, false)) {
                    useFieldRation(self);
                    return;
                }

                if (makeABoom(self)) {
                    return;
                }

                //пытается убить любую вражескую цель, если она убиваема
                if (killAnyEnemyUnit(self)) {
                    return;
                }

                //идёт убивать любую цель, если к ней можно подойти и убить
                if (goAndKillTarget(self)) {
                    return;
                }

                if (tryToUseMedkit(self) && self.getStance() != TrooperStance.PRONE) {
                    return;
                }

                if (self.getHitpoints() <= 65 && goToMedic(self)) {
                    return;
                }

                if (beginBattle) {
                    if (conductTheWar(self)) {
                        return;
                    }
                }

                if (goOnWar(self, targetTrooper.getX(), targetTrooper.getY())) {
                    return;
                }

                if (goToMedic(self)) {
                    return;
                }

                if (self.getActionPoints() < self.getShootCost()) {
                    if (makeValidLowerStance(self, false)) {
                        return;
                    }
                    move.setAction(ActionType.END_TURN);
                    return;
                }

            } else {

                if (throwGrenadeInMirage(self)) {
                    return;
                }

                if (tryToUseMedkit(self)) {
                    return;
                }

                if (goToMedic(self)) {
                    return;
                }

                if (!(istroopersUnderAttack && trooperUnderAttack == self.getId()) && indexOfMedic != -1 && self.getHitpoints() < HP_WHEN_HEAL && isDistanceEqualOrLessOneTail(self, troopers[indexOfMedic])) {
                    if (self.getActionPoints() >= 2) {
                        if (self.getStance() != TrooperStance.PRONE) {
                            move.setAction(ActionType.LOWER_STANCE);
                            return;
                        }
                    }
                    move.setAction(ActionType.END_TURN);
                    return;
                }
            }
        }


        //   @@@@@@@@@@@@@@@@@@@@@@@         СНАЙПЕР             @@@@@@@@@@@@@@@@@@@@@@@@@@@@

        if (self.getType() == TrooperType.SNIPER) {

            underAttack(self);

            if (targetTrooper != null) {

                if (self.getActionPoints() == 6 && self.isHoldingFieldRation() && canSeeOrCanShoot(self, targetTrooper, false)) {
                    useFieldRation(self);
                    return;
                }

                if (makeABoom(self)) {
                    return;
                }

                //пытается убить любую вражескую цель, если она убиваема
                if (killAnyEnemyUnit(self)) {
                    return;
                }

                if (tryToUseMedkit(self)) {
                    return;
                }

                if (tryToUseMedkit(self) && self.getStance() != TrooperStance.PRONE) {
                    return;
                }

                if (self.getHitpoints() <= 65 && goToMedic(self)) {
                    return;
                }

                if (beginBattle) {
                    if (conductTheWar(self)) {
                        return;
                    }
                }

                if (goOnWar(self, targetTrooper.getX(), targetTrooper.getY())) {
                    return;
                }

                if (self.getActionPoints() < self.getShootCost()) {
                    if (makeValidLowerStance(self, false)) {
                        return;
                    }
                    move.setAction(ActionType.END_TURN);
                    return;
                }

            } else {

                if (throwGrenadeInMirage(self)) {
                    return;
                }

                if (tryToUseMedkit(self)) {
                    return;
                }

                if (goToMedic(self)) {
                    return;
                }

                if (!(istroopersUnderAttack && trooperUnderAttack == self.getId()) && indexOfMedic != -1 && self.getHitpoints() < HP_WHEN_HEAL && isDistanceEqualOrLessOneTail(self, troopers[indexOfMedic])) {
                    if (self.getActionPoints() >= 2) {
                        if (self.getStance() != TrooperStance.PRONE) {
                            move.setAction(ActionType.LOWER_STANCE);
                            return;
                        }
                    }
                    move.setAction(ActionType.END_TURN);
                    return;
                }
            }
        }

        //   @@@@@@@@@@@@@@@@@@@@@@@         СТРЕЛЬБА ПО УМОЛЧАНИЮ И ОПРЕДЕЛЕНИЕ ЦЕЛИ            @@@@@@@@@@@@@@@@@@@@@@@@@@@@

        if (self.getActionPoints() >= self.getShootCost()) {
            if (targetTrooper != null) {
                if (canShootOnTarget(self, targetTrooper)) {
                    shootOnTarget(self, targetTrooper);
                    return;
                }
            }
        }

        //Выбор цели, куда нужно идти
        if (targetTrooper != null) {
            moveToTarget(self, targetTrooper.getX(), targetTrooper.getY());
        } else if (localTargetX != 100 && localTargetY != 100) {
            moveToTarget(self, localTargetX, localTargetY);
        } else {
            moveToTarget(self, globalTargetX, globalTargetY);
        }
    }

    // по умолчанию для юнита определяет цель и идёт к ней
    void moveToTarget(Trooper self, int targetX, int targetY) {

        //выбираем цель здесь, проверяем радиусы командора и медика
        if (self.getActionPoints() >= game.getStanceChangeCost()) {

            //встаём, если нет врагов и если находимся не в стоячей позиции
            if (localTargetX == 100 && self.getStance() != TrooperStance.STANDING || ( !(self.getActionPoints() < 6 && self.getStance() == TrooperStance.PRONE || self.getActionPoints() < 4 && self.getStance() == TrooperStance.KNEELING) && (listOfSowEnemys.size() == 0 && self.getActionPoints() >= 6 && (self.getStance() != TrooperStance.STANDING && targetTrooper == null && self.getType() != TrooperType.SNIPER || self.getStance() != TrooperStance.STANDING && self.getType() == TrooperType.SNIPER && targetTrooper == null && !(localTargetX != 100 && world.isVisible(self.getVisionRange(), self.getX(), self.getY(), self.getStance(), localTargetX, localTargetY, TrooperStance.PRONE))))) ) {
                move.setAction(ActionType.RAISE_STANCE);
                return;
            }

            if (self.getDistanceTo(localTargetX, localTargetY) <= 3 && world.isVisible(self.getVisionRange(), self.getX(), self.getY(), self.getStance(), localTargetX, localTargetY, TrooperStance.PRONE)) {
                detectEnemyByTeam = false;
            }

            //TODO вставить здесь расчёт обхода ячейки при следующей =2 (в пути не должно быть в первых 2 клетках =2)
            if ((enemyInAmbush || detectEnemyByTeam) && listOfEnemyTroopers.size() == 0 && targetY == localTargetY && targetX == localTargetX) {
                LinkedList<thePoint> path = lee(self, self.getX(), self.getY(), localTargetX, localTargetY, true);
                LinkedList<thePoint> path1 = lee(self, self.getX(), self.getY(), localTargetX, localTargetY, false);

                if (path != null && path.size() > 1 && path.size() < path1.size() + 5 && trueMapOfPoints[path.get(1).getX()][path.get(1).getY()] == 2 && self.getActionPoints() >= 8) {
                    if (self.getStance() != TrooperStance.STANDING) {
                        move.setAction(ActionType.RAISE_STANCE);
                        return;
                    }

                    if (self.getActionPoints() < 8) {
                        move.setAction(ActionType.END_TURN);
                        return;
                    }

                    if (goOnPath(self, path.get(1).getX(), path.get(1).getY(), true)) {
                        return;
                    }
                } else if (path1 != null && path1.size() > 1 && trueMapOfPoints[path1.get(1).getX()][path1.get(1).getY()] == 2 && self.getActionPoints() >= 8) {
                    if (self.getStance() != TrooperStance.STANDING) {
                        move.setAction(ActionType.RAISE_STANCE);
                        return;
                    }

                    if (self.getActionPoints() < 8) {
                        move.setAction(ActionType.END_TURN);
                        return;
                    }

                    if (goOnPath(self, path1.get(1).getX(), path1.get(1).getY(), false)) {
                        return;
                    }
                }
                                                  //TODO пробую >2 вместо > 4 иначе стоят в 2 клетках и смотрят на эту клетку...
                if (path != null && path.size() > 2 && path.size() < path1.size() + 5 && trueMapOfPoints[path.get(1).getX()][path.get(1).getY()] > 2 && self.getActionPoints() >= 6) {
                    if (goOnPath(self, path.get(1).getX(), path.get(1).getY(), true)) {
                        return;
                    }
                } else if (path1 != null && path1.size() > 2 && trueMapOfPoints[path1.get(1).getX()][path1.get(1).getY()] > 2 && self.getActionPoints() >= 6) {
                    if (goOnPath(self, path1.get(1).getX(), path1.get(1).getY(), false)) {
                        return;
                    }
                }

                if (path != null && path.size() > 2 && path.size() < path1.size() + 5 && trueMapOfPoints[path.get(1).getX()][path.get(1).getY()] < 4 && self.getActionPoints() >= 6) {
                    if (goOnPath(self, path.get(1).getX(), path.get(1).getY(), true)) {
                        return;
                    }
                } else if (path1 != null && path1.size() > 2 && trueMapOfPoints[path1.get(1).getX()][path1.get(1).getY()] < 4 && self.getActionPoints() >= 6) {
                    if (goOnPath(self, path1.get(1).getX(), path1.get(1).getY(), false)) {
                        return;
                    }
                }

                if (path != null && path.size() > 1 && path.size() < path1.size() + 5 && trueMapOfPoints[path.get(0).getX()][path.get(0).getY()] < 4 && self.getActionPoints() >= 6) {
                    if (goOnPath(self, path.get(1).getX(), path.get(1).getY(), true)) {
                        return;
                    }
                } else if (path1 != null && path1.size() > 1 && trueMapOfPoints[path1.get(0).getX()][path1.get(0).getY()] < 4 && self.getActionPoints() >= 6) {
                    if (goOnPath(self, path1.get(1).getX(), path1.get(1).getY(), false)) {
                        return;
                    }
                }

                if (clearSelfArea(self, 4, 2)) {
                    //ничего не делаем
                } else if (clearSelfArea(self, 4, 3)) {
                    if (self.getStance() != TrooperStance.KNEELING) {
                        if (self.getStance() == TrooperStance.STANDING) {
                            move.setAction(ActionType.LOWER_STANCE);
                            return;
                        }
                        if (self.getStance() == TrooperStance.PRONE) {
                            move.setAction(ActionType.RAISE_STANCE);
                            return;
                        }
                    }
                } else {
                    if (self.getStance() != TrooperStance.PRONE) {
                        move.setAction(ActionType.LOWER_STANCE);
                        return;
                    }
                }

                move.setAction(ActionType.END_TURN);
                return;
            }

            if(listOfSowEnemys.size() != 0 && self.getType() == TrooperType.SNIPER) {
                if(self.getDistanceTo(localTargetX, localTargetY) > self.getShootingRange()) {
                    if (self.getDistanceTo(localTargetX, localTargetY) <= self.getShootingRange() + 2 && self.getStance() == TrooperStance.STANDING || self.getDistanceTo(localTargetX, localTargetY) <= self.getShootingRange() + 1 && self.getStance() == TrooperStance.KNEELING) {
                        for (GameUnit gameUnit : listOfSowEnemys) {
                            if (self.getType() != TrooperType.SNIPER && world.isVisible(self.getShootingRange() + 2, self.getX(), self.getY(), self.getStance(), gameUnit.trooper.getX(), gameUnit.trooper.getY(), gameUnit.trooper.getStance()) || self.getType() == TrooperType.SNIPER && world.isVisible(self.getShootingRange() - 2, self.getX(), self.getY(), self.getStance(), gameUnit.trooper.getX(), gameUnit.trooper.getY(), gameUnit.trooper.getStance())) {
                                if(self.getActionPoints() >= game.getStanceChangeCost()) {
                                    move.setAction(ActionType.LOWER_STANCE);
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }

        if (self.getActionPoints() >= getCostMoveWithStance(self)) {
            if (targetX == globalTargetX && targetY == globalTargetY || targetX == localTargetX && targetY == localTargetY) {

                //проверка на попадание в радиус снайпера, если выходишь, возврат обратно
                if (indexOfSniper != -1 && self.getDistanceTo(troopers[indexOfSniper]) > AREA_OF_SNIPER && (indexOfScout != -1 || indexOfSoldier != -1 || indexOfCommander != -1)) {
                    targetX = troopers[indexOfSniper].getX();
                    targetY = troopers[indexOfSniper].getY();
                }

                //проверка на попадание в радиус солдата, если выходишь, возврат обратно
                if (indexOfSoldier != -1 && self.getDistanceTo(troopers[indexOfSoldier]) > AREA_OF_SOLDIER) {
                    targetX = troopers[indexOfSoldier].getX();
                    targetY = troopers[indexOfSoldier].getY();
                }

                //проверка на попадание в радиус командора, если выходишь, возврат обратно
                if (indexOfCommander != -1 && self.getDistanceTo(troopers[indexOfCommander]) > AREA_OF_COMMANDER && self.getType() != TrooperType.SCOUT || indexOfCommander != -1 && self.getDistanceTo(troopers[indexOfCommander]) > 6 && self.getType() == TrooperType.SCOUT) {
                    targetX = troopers[indexOfCommander].getX();
                    targetY = troopers[indexOfCommander].getY();
                }

                //проверка на попадание в радиус командора, если выходишь, возврат обратно
                if (indexOfScout != -1 && self.getDistanceTo(troopers[indexOfScout]) > AREA_OF_SCOUT) {
                    targetX = troopers[indexOfScout].getX();
                    targetY = troopers[indexOfScout].getY();
                }

                // если медик жив и мало хп, то бежим к медику
                if (indexOfMedic != -1 && self.getHitpoints() < HP_WHEN_HEAL && self.getType() != TrooperType.FIELD_MEDIC) {
                    targetX = troopers[indexOfMedic].getX();
                    targetY = troopers[indexOfMedic].getY();
                }
            }


            if (indexOfCommander != -1 && indexOfSniper != -1 && self.getType() == TrooperType.SNIPER && self.getDistanceTo(troopers[indexOfCommander]) > 3) {
                targetX = troopers[indexOfCommander].getX();
                targetY = troopers[indexOfCommander].getY();
            }

            if (indexOfScout != -1 && indexOfSniper != -1 && self.getType() == TrooperType.SNIPER && self.getDistanceTo(troopers[indexOfScout]) > 3) {
                targetX = troopers[indexOfScout].getX();
                targetY = troopers[indexOfScout].getY();
            }

            // если медик жив и мало хп, то бежим к медику
            if (indexOfMedic != -1 && self.getHitpoints() < HP_WHEN_HEAL && self.getType() != TrooperType.FIELD_MEDIC) {
                targetX = troopers[indexOfMedic].getX();
                targetY = troopers[indexOfMedic].getY();
            }

            //чтобы медик не бежал первым!! так как его обзор очень маленький, первым можно бежать если осталось 2 юнита
            if (indexOfSniper != -1 && indexOfMedic != -1 && (indexOfScout != -1 || indexOfCommander != -1 || indexOfSoldier != -1)) {
                if (self.getType() == TrooperType.FIELD_MEDIC && self.getDistanceTo(troopers[indexOfSniper]) >= 3) {
                    if (goOnPath(self, troopers[indexOfSniper].getX(), troopers[indexOfSniper].getY(), false)) {
                        return;
                    }
                }
            }

            if (goOnWar(self, targetX, targetY)) {
                return;
            } else {
                move.setAction(ActionType.END_TURN);
            }
        }
    }

    thePoint closeToTrooper(Trooper self, int targetX, int targetY) {

        //проверка на попадание в радиус командора, если выходишь, возврат обратно
        if (indexOfSniper != -1 && self.getDistanceTo(troopers[indexOfSniper]) > AREA_OF_SNIPER && (indexOfScout != -1 || indexOfSoldier != -1 || indexOfCommander != -1)) {
            targetX = troopers[indexOfSniper].getX();
            targetY = troopers[indexOfSniper].getY();
        }

        //проверка на попадание в радиус солдата, если выходишь, возврат обратно
        if (indexOfSoldier != -1 && self.getDistanceTo(troopers[indexOfSoldier]) > AREA_OF_SOLDIER) {
            targetX = troopers[indexOfSoldier].getX();
            targetY = troopers[indexOfSoldier].getY();
        }

        //проверка на попадание в радиус командора, если выходишь, возврат обратно
        if (indexOfCommander != -1 && self.getDistanceTo(troopers[indexOfCommander]) > AREA_OF_COMMANDER) {
            targetX = troopers[indexOfCommander].getX();
            targetY = troopers[indexOfCommander].getY();
        }

        // если медик жив и мало хп, то бежим к медику
        if (indexOfMedic != -1 && self.getHitpoints() < HP_WHEN_HEAL && self.getType() != TrooperType.FIELD_MEDIC) {
            targetX = troopers[indexOfMedic].getX();
            targetY = troopers[indexOfMedic].getY();
        }

        return new thePoint(targetX, targetY);
    }

    boolean goOnPath(Trooper self, int targetX, int targetY, boolean isWithTroopers) {
        //отвечает за передвежение по полю из точки где находится юнит в указанную точку, путь выбирается через волновой алгоритм lee.
        if (self.getActionPoints() >= getCostMoveWithStance(self)) {
            LinkedList<thePoint> pathOfTrooper;

            if (goToSafePlace) {
                pathOfTrooper = lee(self, self.getX(), self.getY(), targetX, targetY, true);
                if(safePoint != null) {
                    goToBonus = false;
                    bonusTarget = null;
                    if (self.getStance() != TrooperStance.STANDING && !(self.getX() == safePoint.getX() && self.getY() == safePoint.getY())) {
                        move.setAction(ActionType.RAISE_STANCE);
                        return true;
                    }
                    if (safePoint.getX() == self.getX() && safePoint.getY() == self.getY()) {
                        if (self.getActionPoints() >= game.getStanceChangeCost() && (self.getStance() == TrooperStance.STANDING && safeStance == TrooperStance.KNEELING || self.getStance() == TrooperStance.KNEELING && safeStance == TrooperStance.PRONE)) {
                            goToSafePlace = false;
                            safePoint = null;
                            move.setAction(ActionType.LOWER_STANCE);
                            return true;
                        }
                    }
                    pathOfTrooper = lee(self, self.getX(), self.getY(), safePoint.getX(), safePoint.getY(), true);
                }
            } else {
                if (isWithTroopers) {
                    pathOfTrooper = lee(self, self.getX(), self.getY(), targetX, targetY, true);
                } else {
                    pathOfTrooper = lee(self, self.getX(), self.getY(), targetX, targetY, false);
                }
            }

            if (pathOfTrooper != null && pathOfTrooper.size() > 1) {

                if (self.getActionPoints() >= game.getStanceChangeCost() && self.getType() != TrooperType.SNIPER && self.getStance() != TrooperStance.STANDING || self.getActionPoints() >= game.getStanceChangeCost() && self.getType() == TrooperType.SNIPER && self.getStance() == TrooperStance.PRONE) {
                    move.setAction(ActionType.RAISE_STANCE);
                    return true;
                }

                //если юнит идёт к бонусу, AP < 4 и бонус лежит на клетке trueMap которой < 5, то юнит завершает ход
                if (bonusTarget != null && goToBonus && !goToSafePlace && !goThrowGrenade && pathOfTrooper.get(1).getX() == bonusTarget.getX() && pathOfTrooper.get(1).getY() == bonusTarget.getY() && trueMapOfPoints[pathOfTrooper.get(1).getX()][pathOfTrooper.get(1).getY()] < 5 && self.getActionPoints() < 4) {
                    bonusTarget = null;
                    goToBonus = false;
                    move.setAction(ActionType.END_TURN);
                    return true;
                } else if (bonusTarget != null && goToBonus && !goToSafePlace && !goThrowGrenade && pathOfTrooper.get(1).getX() == bonusTarget.getX() && pathOfTrooper.get(1).getY() == bonusTarget.getY()) {
                    bonusTarget = null;
                    goToBonus = false;
                }

                //избегание плохих позиций, если следующая ячейка в таблице trueMapOfPoints == 2, то тогда если её можно обойти за кол-во ходов текущего пути + 5, идём в обход, если нельзя, то встаём и пробуем пройти уже в положении STANDING.
                if (trueMapOfPoints[pathOfTrooper.get(1).getX()][pathOfTrooper.get(1).getY()] == 2 && self.getActionPoints() < 2 * getCostMoveWithStance(self) && !goToSafePlace && !isAfterExplore && !goThrowGrenade && !goToBonus) {
                    if (self.getActionPoints() >= getCostMoveWithStance(self)) {
                        cellsInt[pathOfTrooper.get(1).getX()][pathOfTrooper.get(1).getY()] = -5;
                        LinkedList<thePoint> tempPath = lee(self, self.getX(), self.getY(), targetX, targetY, true);
                        if (self.getType() != TrooperType.SNIPER && tempPath != null && tempPath.size() > 2 && pathOfTrooper.size() + 5 >= tempPath.size() || self.getType() == TrooperType.SNIPER && tempPath != null && tempPath.size() > 2 && pathOfTrooper.size() + 5 >= tempPath.size() && trueMapOfPoints[tempPath.get(2).getX()][tempPath.get(2).getY()] != 2) {
                            pathOfTrooper = tempPath;
                        } else if (self.getType() != TrooperType.SNIPER && self.getStance() != TrooperStance.STANDING && self.getActionPoints() >= game.getStanceChangeCost() || (self.getType() == TrooperType.SNIPER && self.getActionPoints() >= game.getStanceChangeCost() && (self.getStance() != TrooperStance.KNEELING || self.getStance() != TrooperStance.STANDING))) {
                            move.setAction(ActionType.RAISE_STANCE);
                            return true;
                        }
                    } else {
                        move.setAction(ActionType.END_TURN);
                        return true;
                    }
                }

                if (trueMapOfPoints[pathOfTrooper.get(1).getX()][pathOfTrooper.get(1).getY()] == 4 && listOfEnemyTroopers.size() == 0 && self.getActionPoints() >= getCostMoveWithStance(self) && self.getActionPoints() < getCostMoveWithStance(self) * 2 && targetTrooper == null && localTargetX != 100 && !goToSafePlace && !goThrowGrenade && !goToBonus && !isAfterExplore) {
                    move.setAction(ActionType.END_TURN);
                    return true;
                }

                if(self.getActionPoints() < 4 && !goToSafePlace && !goThrowGrenade && !isAfterExplore && trueMapOfPoints[self.getX()][self.getY()] != 2) {
                    move.setAction(ActionType.END_TURN);
                    return true;
                }

                if (isAfterExplore) {
                    LinkedList<thePoint> tempPath = lee(self, self.getX(), self.getY(), targetX, targetY, true);
                    if (tempPath != null && tempPath.size() > 1) {
                        move.setAction(ActionType.MOVE);
                        move.setX(tempPath.get(1).getX());
                        move.setY(tempPath.get(1).getY());

                        if (!testTail(self, tempPath, targetX, targetY)) {
                            return false;
                        }

                        if (!(self.getX() == move.getX() && self.getY() == move.getY())) {
                            lastMoveX = self.getX();
                            lastMoveY = self.getY();
                            complatedPathOfTrooper.add(new thePoint(lastMoveX, lastMoveY));
                            return true;
                        }
                    }
                }

                move.setAction(ActionType.MOVE);
                move.setX(pathOfTrooper.get(1).getX());
                move.setY(pathOfTrooper.get(1).getY());

                if (!testTail(self, pathOfTrooper, targetX, targetY)) {
                    return false;
                }

                if (!goToSafePlace && !goThrowGrenade && !goToBonus && !isGoNear && testOnTrueMap(self, pathOfTrooper)) {
                    return true;
                }

                if (safePoint != null && pathOfTrooper.get(1).getX() == safePoint.getX() && pathOfTrooper.get(1).getY() == safePoint.getY()) {
                    goToSafePlace = false;
                    safePoint = null;
                    idOfTrooperStop = (int) self.getId();
                    saveMoveSafePlace = world.getMoveIndex();
                }

                if (bonusTarget != null && move.getX() == bonusTarget.getX() && move.getY() == bonusTarget.getY()) {
                    bonusTarget = null;
                }

                if (!(self.getX() == move.getX() && self.getY() == move.getY())) {
                    lastMoveX = self.getX();
                    lastMoveY = self.getY();
                    complatedPathOfTrooper.add(new thePoint(lastMoveX, lastMoveY));
                    if (isGoNear) {
                        isGoNear = false;
                    }
                    return true;
                }
            }
        }

        return false;
    }

    //когда обнаружены чужие юниты, то начинает подходить, стрелять, отходить, контролирует параметры очков хода
    boolean goOnWar(Trooper self, int targetX, int targetY) {

        //медик при обнаружении врага пытается его добить если мало хп и бежит к дальнему своему юниту.
        if (self.getType() == TrooperType.FIELD_MEDIC && listOfEnemyTroopers != null && listOfEnemyTroopers.size() != 0) {

            //пытается убить любую вражескую цель, если она убиваема
            if (killAnyEnemyUnit(self)) {
                return true;
            }

            if (goAndKillTarget(self)) {
                return true;
            }

            /*if (targetHeal == null) {*/
                if (indexOfSniper != -1 && self.getDistanceTo(troopers[indexOfSniper]) > 3) {

                    for (Trooper trooper : listOfEnemyTroopers) {

                        if (canSeeOrCanShoot(self, trooper, false)) {
                            if (troopers[indexOfSniper].getHitpoints() <= 50) {
                                targetHeal = troopers[indexOfSniper];
                                if (goOnPath(self, troopers[indexOfSniper].getX(), troopers[indexOfSniper].getY(), false)) {
                                    return true;
                                }
                            }
                        }

                    }

                } else if (indexOfSniper == -1 && indexOfSoldier != -1 && self.getDistanceTo(troopers[indexOfSoldier]) > 2) {
                    for (Trooper trooper : listOfEnemyTroopers) {
                        if (canSeeOrCanShoot(self, trooper, false)) {
                            if (troopers[indexOfSoldier].getHitpoints() <= 50) {
                                targetHeal = troopers[indexOfSoldier];
                                if (goOnPath(self, troopers[indexOfSoldier].getX(), troopers[indexOfSoldier].getY(), false)) {
                                    return true;
                                }
                            }
                        }
                    }
                } else if (indexOfSniper == -1 && indexOfSoldier == -1 && indexOfCommander != -1 && self.getDistanceTo(troopers[indexOfCommander]) > 2) {
                    for (Trooper trooper : listOfEnemyTroopers) {
                        if (canSeeOrCanShoot(self, trooper, false)) {
                            if (troopers[indexOfCommander].getHitpoints() <= 50) {
                                targetHeal = troopers[indexOfCommander];
                                if (goOnPath(self, troopers[indexOfCommander].getX(), troopers[indexOfCommander].getY(), false)) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            /*}*/
        }

        //управление снайпером, чтобы не лез на передовую при живых командире и солдате
        if (self.getType() == TrooperType.SNIPER && listOfEnemyTroopers != null && listOfEnemyTroopers.size() > 0 && (indexOfCommander != -1 || indexOfScout != -1 || indexOfSoldier != -1)) {

            //пытается убить любую вражескую цель, если она убиваема
            if (killAnyEnemyUnit(self)) {
                return true;
            }

            if (localTargetX == 100 && indexOfCommander != -1 && self.getDistanceTo(troopers[indexOfCommander]) > 3) {
                if (goOnPath(self, troopers[indexOfCommander].getX(), troopers[indexOfCommander].getY(), false)) {
                    return true;
                }
            } else if (localTargetX == 100 && indexOfCommander == -1 && indexOfScout != -1 && self.getDistanceTo(troopers[indexOfScout]) > 3) {
                if (goOnPath(self, troopers[indexOfScout].getX(), troopers[indexOfScout].getY(), false)) {
                    return true;
                }
            } else if (localTargetX == 100 && indexOfCommander == -1 && indexOfScout == -1 && indexOfSoldier != -1 && self.getDistanceTo(troopers[indexOfSoldier]) > 3) {
                if (goOnPath(self, troopers[indexOfSoldier].getX(), troopers[indexOfSoldier].getY(), false)) {
                    return true;
                }
            }
        }

        Trooper choosenOne;
        if (listOfEnemyTroopers.size() != 0) {

            choosenOne = chooseEnemyOnDistance(self, listOfEnemyTroopers);
            if(choosenOne != null) {
                targetTrooper = choosenOne;
            }

            detectEnemyByTeam = true;

            boolean isVisibleForEnemys = false;

            //TODO сделать trooperUnderAttack списком, так как несколько юнитов могут быть под атакой!
            if(istroopersUnderAttack && trooperUnderAttack == self.getId()) {
                //если расстояние до врага меньше видимости юнита, то идти как обычно, если юнит не видит никого, то отходить
                boolean flag = false;
                for (Trooper trooper : listOfEnemyTroopers) {
                    if (!trooper.isTeammate() && self.getDistanceTo(trooper) <= self.getVisionRange() && world.isVisible(self.getVisionRange(), self.getX(), self.getY(), TrooperStance.STANDING, trooper.getX(), trooper.getY(), trooper.getStance())) {
                        flag = true;
                        break;
                    }
                }
                if(!flag) {
                    isVisibleForEnemys = true;
                } else {
                    isVisibleForEnemys = false;
                }
            }

            for (Trooper trooper : listOfEnemyTroopers) {
                if (world.isVisible(trooper.getVisionRange(), trooper.getX(), trooper.getY(), TrooperStance.STANDING, self.getX(), self.getY(), self.getStance())) {
                    isVisibleForEnemys = true;
                    break;
                }
            }

            //логика обработки врагов. Если врагов: 1 ...
            if (targetTrooper != null && listOfEnemyTroopers.size() == 1 && isVisibleForEnemys) {

                if(self.getStance() == TrooperStance.PRONE || self.getType() == TrooperType.FIELD_MEDIC) {
                    for(Trooper trooper : listOfEnemys) {
                        if(canSeeOrCanShoot(self, trooper, false) && self.getActionPoints() >= /*2 **/ self.getShootCost()) {
                            if (self.getType() == TrooperType.FIELD_MEDIC && makeValidLowerStance(self, true)) {
                                return true;
                            }
                            shootOnTarget(self, trooper);
                            return true;
                        }
                    }
                }

                if (goToMedic(self)) {
                    return true;
                }

                if(world.getMoveIndex() > 5) {
                    if ((indexOfSoldier != -1 && self.getType() != TrooperType.SOLDIER && troopers[indexOfSoldier].getDistanceTo(targetTrooper) <= troopers[indexOfSoldier].getShootingRange() && world.isVisible(troopers[indexOfSoldier].getVisionRange(), troopers[indexOfSoldier].getX(), troopers[indexOfSoldier].getY(), troopers[indexOfSoldier].getStance(), targetTrooper.getX(), targetTrooper.getY(), targetTrooper.getStance())) || (indexOfSniper != -1 && self.getType() != TrooperType.SNIPER && troopers[indexOfSniper].getDistanceTo(targetTrooper) <= troopers[indexOfSniper].getShootingRange() && world.isVisible(troopers[indexOfSniper].getVisionRange(), troopers[indexOfSniper].getX(), troopers[indexOfSniper].getY(), troopers[indexOfSniper].getStance(), targetTrooper.getX(), targetTrooper.getY(), targetTrooper.getStance()))) {
                        Trooper trooper = listOfEnemyTroopers.get(0);
                        if (teamSupportCount > 1 && trooper.getStance() != TrooperStance.PRONE && self.getHitpoints() > 65 && trooper.getType() == TrooperType.SOLDIER || teamSupportCount > 1 && trooper.getStance() != TrooperStance.PRONE && trooper.getType() != TrooperType.SOLDIER && self.getHitpoints() > 65 || trueMapOfPoints[self.getX()][self.getY()] != 2 && trooper.getHitpoints() <= 30 && trooper.getStance() == TrooperStance.PRONE || trueMapOfPoints[self.getX()][self.getY()] != 2 && self.getHitpoints() > 65 && trooper.getHitpoints() <= 75 && trooper.getStance() != TrooperStance.PRONE && Math.random() * 3 == 0) {
                            isVisibleForEnemys = false;
                            goToSafePlace = false;
                            safePoint = null;
                            saveMoveSafePlace = -1;
                            savedTrooperId = -1;
                        }
                    }
                }
            }

            //... или 2 ...
            if (listOfEnemyTroopers.size() == 2 && isVisibleForEnemys && (targetTrooper != null && self.getActionPoints() >=6 || self.getStance() != TrooperStance.STANDING)) {

                if(self.getStance() == TrooperStance.PRONE || self.getType() == TrooperType.FIELD_MEDIC) {
                    for(Trooper trooper : listOfEnemys) {
                        if(canSeeOrCanShoot(self, trooper, false) && self.getActionPoints() >= /*2 **/ self.getShootCost()) {
                            if (self.getType() == TrooperType.FIELD_MEDIC && makeValidLowerStance(self, true)) {
                                return true;
                            }
                            shootOnTarget(self, trooper);
                            return true;
                        }
                    }
                }

                if (goToMedic(self)) {
                    return true;
                }

                if(world.getMoveIndex() > 5) {
                    for (Trooper trooper : listOfEnemyTroopers) {
                        if (teamSupportCount > 2 && trooper.getStance() != TrooperStance.PRONE && self.getHitpoints() > 75 && trooper.getType() == TrooperType.SOLDIER || teamSupportCount > 2 && trooper.getStance() != TrooperStance.PRONE && trooper.getType() != TrooperType.SOLDIER && self.getHitpoints() > 65 || trueMapOfPoints[self.getX()][self.getY()] != 2 && trooper.getHitpoints() <= 30 && trooper.getStance() == TrooperStance.PRONE || trueMapOfPoints[self.getX()][self.getY()] != 2 && self.getHitpoints() > 75 && trooper.getHitpoints() <= 75 && trooper.getStance() != TrooperStance.PRONE && Math.random() * 4 == 0) {
                            isVisibleForEnemys = false;
                            goToSafePlace = false;
                            safePoint = null;
                            saveMoveSafePlace = -1;
                            savedTrooperId = -1;
                            break;
                        }
                    }
                }
            }

            //... или >= 3.
            if (listOfEnemyTroopers.size() >= 3 && isVisibleForEnemys && (targetTrooper != null || self.getStance() != TrooperStance.STANDING)) {

                if(self.getStance() == TrooperStance.PRONE || self.getType() == TrooperType.FIELD_MEDIC) {
                    for (Trooper trooper : listOfEnemys) {
                        if (canSeeOrCanShoot(self, trooper, false) && self.getActionPoints() >= /*2 **/ self.getShootCost()) {
                            if (self.getType() == TrooperType.FIELD_MEDIC && makeValidLowerStance(self, true)) {
                                return true;
                            }
                            shootOnTarget(self, trooper);
                            return true;
                        }
                    }
                }

                if (goToMedic(self)) {
                    return true;
                }

                if (self.getHitpoints() > 75) {
                    if (teamSupportCount > 2) {
                        isVisibleForEnemys = false;
                        goToSafePlace = false;
                        safePoint = null;
                        saveMoveSafePlace = -1;
                        savedTrooperId = -1;
                    }
                }
            }

            if(isVisibleForEnemys && (self.getType() != TrooperType.SNIPER || self.getType() == TrooperType.SNIPER && self.getActionPoints() < self.getShootCost())) {

                if (targetTrooper != null && shootAndGoToSafePlace(self, targetTrooper, true)) {    //обрабатываем стрельбу и убегание на невидимую ячейку
                    return true;
                } else if (targetTrooper != null && shootAndGoToSafePlace(self, targetTrooper, false)) { //обрабатываем стрельбу и убегание на недосягаемую ячейку
                    return true;
                } else {
                    //пробуем присесть
                    if(targetTrooper != null) {
                        if (self.getShootingRange() >= self.getDistanceTo(targetTrooper)) {

                            boolean needGoDown1 = false;
                            boolean needGoDown2 = false;
                            boolean needGoDown3 = false;

                            if (self.getStance() == TrooperStance.STANDING) {
                                needGoDown1 = self.getActionPoints() >= 4 && !world.isVisible(targetTrooper.getVisionRange(), targetTrooper.getX(), targetTrooper.getY(), targetTrooper.getStance(), self.getX(), self.getY(), TrooperStance.PRONE);
                                needGoDown2 = self.getActionPoints() >= 2 && !world.isVisible(targetTrooper.getVisionRange(), targetTrooper.getX(), targetTrooper.getY(), targetTrooper.getStance(), self.getX(), self.getY(), TrooperStance.KNEELING);
                            } else if (self.getStance() == TrooperStance.KNEELING) {
                                needGoDown3 = self.getActionPoints() >= 2 && !world.isVisible(targetTrooper.getVisionRange(), targetTrooper.getX(), targetTrooper.getY(), targetTrooper.getStance(), self.getX(), self.getY(), TrooperStance.PRONE);
                            } else {
                                needGoDown1 = false;
                                needGoDown3 = false;
                            }
                            if (needGoDown1 || needGoDown2 || needGoDown3) {
                                if (self.getActionPoints() >= self.getShootCost() + 2 * game.getStanceChangeCost() && needGoDown1) {
                                    if(canShootOnTarget(self,targetTrooper)) {
                                        shootOnTarget(self, targetTrooper);
                                        return true;
                                    }
                                }
                                if (self.getActionPoints() >= self.getShootCost() + game.getStanceChangeCost() && needGoDown2) {
                                    if(canShootOnTarget(self,targetTrooper)) {
                                        shootOnTarget(self, targetTrooper);
                                        return true;
                                    }
                                }
                                if (self.getActionPoints() >= self.getShootCost() + game.getStanceChangeCost() && needGoDown3) {
                                    if(canShootOnTarget(self,targetTrooper)) {
                                        shootOnTarget(self, targetTrooper);
                                        return true;
                                    }
                                }

                                if(self.getActionPoints() >= game.getStanceChangeCost()) {
                                    move.setAction(ActionType.LOWER_STANCE);
                                    return true;
                                }
                            }
                        }
                    }

                    for (Trooper trooper : listOfEnemyTroopers) {
                        if (canSeeOrCanShoot(trooper, self, false)) {
                            if (conductTheWar(self)) {
                                return true;
                            }
                        }
                    }
                }
            }

            if (killAnyEnemyUnit(self)) {
                return true;
            }

            if (self.getType() == TrooperType.SNIPER) {

                if (targetTrooper != null && testSniperShootingThenMoveDown(self, targetTrooper)) {
                    return true;
                } else {

                    if (targetTrooper != null && canShootOnTarget(self, targetTrooper)) {
                        if (makeValidLowerStance(self, true)) {
                            return true;
                        }
                        shootOnTarget(self, targetTrooper);
                        return true;
                    }

                    if (listOfEnemyTroopers.size() != 0) {
                        for (Trooper trooper : listOfEnemyTroopers) {

                            if (testSniperShootingThenMoveDown(self, trooper)) {
                                return true;
                            } else if (canShootOnTarget(self, trooper)) {

                                if (makeValidLowerStance(self, true)) {
                                    return true;
                                }
                                shootOnTarget(self, trooper);
                                return true;

                            }
                        }
                    }
                }

            }

            if (targetTrooper != null && self.getActionPoints() >= game.getStanceChangeCost() + self.getShootCost() && (canShootOnTarget(self, targetTrooper) || self.getStance() != TrooperStance.STANDING && testMoveUpAttack(self, targetTrooper))) {
                if (self.getActionPoints() >= getCostMoveWithStance(self)) {
                    if (testForFreePassage(self)) {
                        return true;
                    }
                }

                if (testMoveUpAttack(self, targetTrooper)) {
                    return true;
                }

                 if (makeValidLowerStance(self, true)) {
                    return true;
                }

                shootOnTarget(self, targetTrooper);
                return true;
            } else {
                // если снайперу не хватает AP на стрельбу и мишень в зоне досягаемости, то он пытается леч
                if (targetTrooper != null && self.getType() == TrooperType.SNIPER && self.getActionPoints() < 9 && canSeeOrCanShoot(self, targetTrooper, false)) {
                    if (makeValidLowerStance(self, false)) {
                        return true;
                    }
                }
            }

            if (targetTrooper != null && self.getActionPoints() >= game.getStanceChangeCost() && !world.isVisible(self.getShootingRange(), self.getX(), self.getY(), self.getStance(), targetTrooper.getX(), targetTrooper.getY(), targetTrooper.getStance())) {

                if (self.getStance() != TrooperStance.STANDING && testMoveUpAttack(self, targetTrooper)) {
                    return true;
                }

                if (self.getActionPoints() >= self.getShootCost()) {
                    for (Trooper trooper : listOfEnemyTroopers) {
                        if (canShootOnTarget(self, trooper)) {
                            shootOnTarget(self, trooper);
                            return true;
                        }
                    }
                }

                //если юнит (не солдат и не снайпер) видит врага и снайпер или солдат дальше на 2 от данного вражеского юнита, то трупер отходит или по пройденому собой пути или по координатам ближайшего из солдата или снайпера.
                if (self.getType() != TrooperType.SNIPER && self.getType() != TrooperType.SOLDIER && (indexOfSoldier != -1 && troopers[indexOfSoldier].getDistanceTo(targetTrooper) - 2 > self.getDistanceTo(targetTrooper) && indexOfSniper != -1 && troopers[indexOfSniper].getDistanceTo(targetTrooper) - 2 > self.getDistanceTo(targetTrooper))) {
                    if (complatedPathOfTrooper != null && complatedPathOfTrooper.size() >= 3) {
                        goToSafePlace = true;
                        safePoint = new thePoint(complatedPathOfTrooper.get(complatedPathOfTrooper.size() - 2).getX(), complatedPathOfTrooper.get(complatedPathOfTrooper.size() - 2).getY());
                    } else {
                        LinkedList<thePoint> path1 =lee(self, self.getX(), self.getY(), troopers[indexOfSniper].getX(), troopers[indexOfSniper].getY(), false);
                        LinkedList<thePoint> path2 =lee(self, self.getX(), self.getY(), troopers[indexOfSoldier].getX(), troopers[indexOfSoldier].getY(), false);

                        if (path1.size() >= path2.size()) {
                            safePoint = new thePoint(troopers[indexOfSniper].getX(), troopers[indexOfSniper].getY());
                        } else if(path1.size() < path2.size()) {
                            safePoint = new thePoint(troopers[indexOfSoldier].getX(), troopers[indexOfSoldier].getY());
                        }
                    }

                    if (goOnPath(self, safePoint.getX(), safePoint.getY(), false)) {
                        return true;
                    }
                }

                LinkedList<thePoint> path = lee(self, self.getX(), self.getY(), targetTrooper.getX(), targetTrooper.getY(), true);
                LinkedList<thePoint> path1 = lee(self, self.getX(), self.getY(), targetTrooper.getX(), targetTrooper.getY(), false);

                //расчёт безопасной ячейки из которой можно выстрелить по одному из вражеских юнитов (путь не должен содержать в себе видимого для врагов отрезка, который нельзя пройти не остановившись в нём)
                thePoint pointOfWar = findPointForAttackEnemyTroopers(self);
                if (pointOfWar != null) {
                    LinkedList<thePoint> path3 = lee(self, self.getX(), self.getY(), pointOfWar.getX(), pointOfWar.getY(), true);
                    if (path3 != null && goOnPath(self, pointOfWar.getX(), pointOfWar.getY(), true)) {
                        return true;
                    }
                }

                if (path != null && path.size() > 1 || path1 != null && path1.size() > 1) {

                    thePoint point = null;

                    if (self.getDistanceTo(targetTrooper) <= self.getShootingRange() ? true : self.getDistanceTo(targetTrooper) + 5 < path1.size()) {
                        point = findClosestPointForEnemyTroopers(self, targetTrooper);
                    }

                    if (point != null && goOnPath(self, point.getX(), point.getY(), true) && self.getActionPoints() >= 6) {
                        return true;
                    } else {

                        if (path != null && path.size() < path1.size() + 5) {

                            if (path.size() > 1 && trueMapOfPoints[path.get(1).getX()][path.get(1).getY()] == 2 && self.getActionPoints() >= 8) {
                                if (self.getStance() != TrooperStance.STANDING) {
                                    move.setAction(ActionType.RAISE_STANCE);
                                    return true;
                                }

                                if (self.getActionPoints() < 8) {
                                    if (clearSelfArea(self, 4, 2)) {
                                        //ничего не делаем
                                    } else if (clearSelfArea(self, 4, 3)) {
                                        if (self.getStance() != TrooperStance.KNEELING) {
                                            if (self.getStance() == TrooperStance.STANDING) {
                                                move.setAction(ActionType.LOWER_STANCE);
                                                return true;
                                            }
                                            if (self.getStance() == TrooperStance.PRONE) {
                                                move.setAction(ActionType.RAISE_STANCE);
                                                return true;
                                            }
                                        }
                                    } else {
                                        if (self.getStance() != TrooperStance.PRONE) {
                                            move.setAction(ActionType.LOWER_STANCE);
                                            return true;
                                        }
                                    }
                                    move.setAction(ActionType.END_TURN);
                                    return true;
                                }

                                if (goOnPath(self, path.get(1).getX(), path.get(1).getY(), true)) {
                                    return true;
                                }
                            }

                            if (path.size() > 4 && trueMapOfPoints[path.get(1).getX()][path.get(1).getY()] > 2 && self.getActionPoints() >= 6 && !world.isVisible(self.getVisionRange() - 1, self.getX(), self.getY(), self.getStance(), targetTrooper.getX(), targetTrooper.getY(), TrooperStance.PRONE)) {
                                if (goOnPath(self, path.get(1).getX(), path.get(1).getY(), true)) {
                                    return true;
                                }
                            }

                            if (path.size() > 4 && trueMapOfPoints[path.get(1).getX()][path.get(1).getY()] < 4 && self.getActionPoints() >= 6 && !world.isVisible(self.getVisionRange() - 1, self.getX(), self.getY(), self.getStance(), targetTrooper.getX(), targetTrooper.getY(), TrooperStance.PRONE)) {
                                if (goOnPath(self, path.get(1).getX(), path.get(1).getY(), true)) {
                                    return true;
                                }
                            }

                            if (path.size() > 1 && trueMapOfPoints[path.get(0).getX()][path.get(0).getY()] < 4 && self.getActionPoints() >= 6 && !world.isVisible(self.getVisionRange() - 1, self.getX(), self.getY(), self.getStance(), targetTrooper.getX(), targetTrooper.getY(), TrooperStance.PRONE)) {
                                if (goOnPath(self, path.get(1).getX(), path.get(1).getY(), true)) {
                                    return true;
                                }
                            }

                            if (clearSelfArea(self, 4, 2)) {
                                //ничего не делаем
                            } else if (clearSelfArea(self, 4, 3)) {
                                if (self.getStance() != TrooperStance.KNEELING) {
                                    if (self.getStance() == TrooperStance.STANDING) {
                                        move.setAction(ActionType.LOWER_STANCE);
                                        return true;
                                    }
                                    if (self.getStance() == TrooperStance.PRONE) {
                                        move.setAction(ActionType.RAISE_STANCE);
                                        return true;
                                    }
                                }
                            } else {
                                if (self.getStance() != TrooperStance.PRONE) {
                                    move.setAction(ActionType.LOWER_STANCE);
                                    return true;
                                }
                            }

                            move.setAction(ActionType.END_TURN);
                            return true;

                        } else {

                            if (path1 != null && path1.size() > 1 && trueMapOfPoints[path1.get(1).getX()][path1.get(1).getY()] == 2 && self.getActionPoints() >= 8) {
                                if (self.getStance() != TrooperStance.STANDING) {
                                    move.setAction(ActionType.RAISE_STANCE);
                                    return true;
                                }

                                if (self.getActionPoints() < 8) {
                                    if (clearSelfArea(self, 4, 2)) {
                                        //ничего не делаем
                                    } else if (clearSelfArea(self, 4, 3)) {
                                        if (self.getStance() != TrooperStance.KNEELING) {
                                            if (self.getStance() == TrooperStance.STANDING) {
                                                move.setAction(ActionType.LOWER_STANCE);
                                                return true;
                                            }
                                            if (self.getStance() == TrooperStance.PRONE) {
                                                move.setAction(ActionType.RAISE_STANCE);
                                                return true;
                                            }
                                        }
                                    } else {
                                        if (self.getStance() != TrooperStance.PRONE) {
                                            move.setAction(ActionType.LOWER_STANCE);
                                            return true;
                                        }
                                    }
                                    move.setAction(ActionType.END_TURN);
                                    return true;
                                }

                                if (goOnPath(self, path1.get(1).getX(), path1.get(1).getY(), false)) {
                                    return true;
                                }
                            }

                            if (path1.size() > 4 && trueMapOfPoints[path1.get(1).getX()][path1.get(1).getY()] > 2 && self.getActionPoints() >= 6 && !world.isVisible(self.getVisionRange(), self.getX(), self.getY(), self.getStance(), targetTrooper.getX(), targetTrooper.getY(), TrooperStance.PRONE)) {
                                if (goOnPath(self, path1.get(1).getX(), path1.get(1).getY(), true)) {
                                    return true;
                                }
                            }

                            if (path1.size() > 4 && trueMapOfPoints[path1.get(1).getX()][path1.get(1).getY()] < 4 && self.getActionPoints() >= 8 && !world.isVisible(self.getVisionRange(), self.getX(), self.getY(), self.getStance(), targetTrooper.getX(), targetTrooper.getY(), TrooperStance.PRONE)) {
                                if (goOnPath(self, path1.get(1).getX(), path1.get(1).getY(), true)) {
                                    return true;
                                }
                            }

                            if (path1.size() > 1 && trueMapOfPoints[path1.get(0).getX()][path1.get(0).getY()] < 4 && self.getActionPoints() >= 6 && !world.isVisible(self.getVisionRange(), self.getX(), self.getY(), self.getStance(), targetTrooper.getX(), targetTrooper.getY(), TrooperStance.PRONE)) {
                                if (goOnPath(self, path1.get(1).getX(), path1.get(1).getY(), true)) {
                                    return true;
                                }
                            }

                                if (clearSelfArea(self, 4, 2)) {
                                    //ничего не делаем
                                } else if (clearSelfArea(self, 4, 3)) {
                                    if (self.getStance() != TrooperStance.KNEELING) {
                                        if (self.getStance() == TrooperStance.STANDING) {
                                            move.setAction(ActionType.LOWER_STANCE);
                                            return true;
                                        }
                                        if (self.getStance() == TrooperStance.PRONE) {
                                            move.setAction(ActionType.RAISE_STANCE);
                                            return true;
                                        }
                                    }
                                } else {
                                    if (self.getStance() != TrooperStance.PRONE) {
                                        move.setAction(ActionType.LOWER_STANCE);
                                        return true;
                                    }
                                }

                            move.setAction(ActionType.END_TURN);
                            return true;
                        }
                    }

                }

                if (goOnPath(self, targetTrooper.getX(), targetTrooper.getY(), false)) {
                    return true;
                }
            }

            if(self.getType() == TrooperType.FIELD_MEDIC) {
                if (goHeal(self)) {
                    return true;
                }
            }

            if (targetTrooper != null && isBetweenWalls(self.getX(), self.getY()) && self.getActionPoints() >= 6) {
                if (goOnPath(self, targetTrooper.getX(), targetTrooper.getY(), false)) {
                    return true;
                }
            }

        } else {

            //идём в точку спасения или ложимся, если так можно спрятаться
            if (safePoint != null) {
                if (safePoint.getX() == self.getX() && safePoint.getY() == self.getY()) {
                    if(self.getActionPoints() >= game.getStanceChangeCost()) {
                        move.setAction(ActionType.LOWER_STANCE);
                        return true;
                    }
                }
                LinkedList<thePoint> safePath = lee(self, self.getX(), self.getY(), safePoint.getX(), safePoint.getY(), true);
                if (safePath != null && safePath.size() > 1 && self.getActionPoints() >= (safePath.size() - 1) * getCostMoveWithStance(self)) {
                    if (goOnPath(self, safePoint.getX(), safePoint.getY(), true)) {
                        return true;
                    }
                }
            }

            //стреляем по месту, где видели противника
            if (listOfSowEnemys.size() != 0) {
                GameUnit targetUnit = null;
                if (self.getActionPoints() >= self.getShootCost()) {
                    for (GameUnit gameUnit: listOfSowEnemys) {
                        if (self.getDistanceTo(gameUnit.trooper.getX(), gameUnit.trooper.getY()) <= self.getShootingRange() && world.isVisible(self.getShootingRange(), self.getX(), self.getY(), self.getStance(), gameUnit.trooper.getX(), gameUnit.trooper.getY(), gameUnit.trooper.getStance())) {
                            if (targetUnit == null) {
                                targetUnit = gameUnit;
                            } else if (targetUnit.trooper.getHitpoints() > gameUnit.trooper.getHitpoints()) {
                                targetUnit = gameUnit;
                            }
                        }
                    }
                }

                if (targetUnit != null) {
                    isShootingAnywhere = true;
                    targetUnitIdSave = (int) targetUnit.trooper.getId();

                    for (Player player : world.getPlayers()) {
                        if (player.getName().equalsIgnoreCase("darkstone")) {
                            myScore = player.getScore();
                            break;
                        }
                    }

                    if (testMoveUpAttack(self, targetUnit.trooper)) {
                        return true;
                    }

                    move.setAction(ActionType.SHOOT);
                    move.setX(targetUnit.trooper.getX());
                    move.setY(targetUnit.trooper.getY());
                    return true;
                }
            }

            //обработка бонусов, их подбор если нет в наличии
            if (bonuses != null) {
                if (bonusTarget != null && self.getActionPoints() > 4) {
                    if (goOnPath(self, bonusTarget.getX(), bonusTarget.getY(), true)) {
                        return true;
                    }
                } else if (bonusTarget != null) {
                    thePoint point = closeToTrooper(self, targetX, targetY);
                    targetX = point.getX();
                    targetY = point.getY();
                }

                if(bonusTarget == null) {

                    for (Bonus bonus : bonuses) {

                        boolean isGoToBonus = true;

                        for (Trooper troop : troopers) {
                            if (troop.getX() == bonus.getX() && troop.getY() == bonus.getY()) {
                                isGoToBonus = false;
                            }
                        }

                        LinkedList<thePoint> pathTemp = lee(self, self.getX(), self.getY(), bonus.getX(), bonus.getY(), true);
                        if (pathTemp != null && pathTemp.size() > 4 && forwardTrooper == -1 || pathTemp != null && pathTemp.size() > 4 && forwardTrooper != -1 && troopers[forwardTrooper].getDistanceTo(bonus) > 3 || self.getActionPoints() < 4) {
                            isGoToBonus = false;
                        }

                        if (isGoToBonus && self.getDistanceTo(bonus) <= 3 && !self.isHoldingGrenade() && bonus.getType() == BonusType.GRENADE) {
                            goToBonus = true;
                            bonusTarget = bonus;
                            if (goOnPath(self, bonus.getX(), bonus.getY(), true)) {
                                return true;
                            }
                        }
                        if (isGoToBonus && self.getDistanceTo(bonus) <= 3 && !self.isHoldingMedikit() && bonus.getType() == BonusType.MEDIKIT) {
                            goToBonus = true;
                            bonusTarget = bonus;
                            if (goOnPath(self, bonus.getX(), bonus.getY(), true)) {
                                return true;
                            }
                        }
                        if (isGoToBonus && self.getDistanceTo(bonus) <= 3 && !self.isHoldingFieldRation() && bonus.getType() == BonusType.FIELD_RATION) {
                            goToBonus = true;
                            bonusTarget = bonus;
                            if (goOnPath(self, bonus.getX(), bonus.getY(), true)) {
                                return true;
                            }
                        }

                    }

                }
            }

            if (targetTrooper == null && self.getActionPoints() >= getCostMoveWithStance(self) * 3 && self.getType() == TrooperType.SNIPER && !goToSafePlace && targetHeal == null && (targetX == localTargetX && targetY == localTargetY || targetX == globalTargetX && targetY == globalTargetY)) {
                if (followAfterTrooper(self, targetX, targetY)) {
                    return true;
                }
            }
            if (targetTrooper == null &&  self.getActionPoints() >= getCostMoveWithStance(self) * 3 && self.getType() == TrooperType.FIELD_MEDIC && !goToSafePlace && targetHeal == null && (targetX == localTargetX && targetY == localTargetY || targetX == globalTargetX && targetY == globalTargetY)) {
                if (followAfterTrooper(self, targetX, targetY)) {
                    return true;
                }
            }
            if (targetTrooper == null &&  self.getActionPoints() >= getCostMoveWithStance(self) * 3 && self.getType() == TrooperType.SOLDIER && !goToSafePlace && targetHeal == null && (targetX == localTargetX && targetY == localTargetY || targetX == globalTargetX && targetY == globalTargetY)) {
                if (followAfterTrooper(self, targetX, targetY)) {
                    return true;
                }
            }
            if (targetTrooper == null &&  self.getActionPoints() >= getCostMoveWithStance(self) * 3 && self.getType() == TrooperType.COMMANDER && !goToSafePlace && targetHeal == null && (targetX == localTargetX && targetY == localTargetY || targetX == globalTargetX && targetY == globalTargetY)) {
                if (followAfterTrooper(self, targetX, targetY)) {
                    return true;
                }
            }

            //если нет врагов, собираемся возле покоцанного юнита и ждём пока все отхиляются
            if (targetHeal != null && indexOfMedic != -1 && self.getDistanceTo(troopers[indexOfMedic]) > 3 && self.getType() != TrooperType.FIELD_MEDIC) {
                if (goOnPath(self, targetHeal.getX(), targetHeal.getY(), false)) {
                    return true;
                }
            }

            if (self.getActionPoints() >= getCostMoveWithStance(self) * 4) {
                if (goOnPath(self, targetX, targetY, false)) {
                    return true;
                }
            }

            if (self.getType() == TrooperType.SNIPER && self.getActionPoints() >= 6 && self.getStance() != TrooperStance.STANDING/* && targetX == localTargetX && targetY == localTargetY*/) {

                if (listOfSowEnemys.size() != 0) {
                    for (GameUnit gameUnit : listOfSowEnemys) {
                        if (testMoveUpAttack(self, gameUnit.trooper)) {
                            return true;
                        }
                    }
                }
                //поднимаю снайпера, чтобы он полз на коленях
                if (self.getStance() == TrooperStance.PRONE) {
                    move.setAction(ActionType.RAISE_STANCE);
                    return true;
                }

                if (goOnPath(self, targetX, targetY, false)) {
                    return true;
                }
            }

            if (localTargetX != 100 && self.getType() == TrooperType.SNIPER && self.getActionPoints() >= getCostMoveWithStance(self) && !world.isVisible(/*AREA_OF_SNIPER self.getVisionRange()*/self.getShootingRange(), self.getX(), self.getY(), self.getStance(), localTargetX, localTargetY, TrooperStance.STANDING)) {

                if (goOnPath(self, localTargetX, localTargetY, false)) {
                    return true;
                }
            }

            if (isBetweenWalls(self.getX(), self.getY()) && self.getActionPoints() >= 6) {
                if (goOnPath(self, targetX, targetY, false)) {
                    return true;
                }
            }

            if (isBetweenWalls(lastMoveX, lastMoveY) && trueMapOfPoints[self.getX()][self.getY()] == 4 && self.getActionPoints() >= getCostMoveWithStance(self)) {
                if (goOnPath(self, targetX, targetY, false)) {
                    return true;
                }
            }

            if (trueMapOfPoints[self.getX()][self.getY()] == 2 && self.getActionPoints() >= getCostMoveWithStance(self)) {
                thePoint tempPoint = findStrongCell(self, self.getX(), self.getY(), 4);
                if (tempPoint != null) {
                    if (goOnPath(self, tempPoint.getX(), tempPoint.getY(), true)) {
                        return true;
                    }
                } else {
                    tempPoint = findStrongCell(self, self.getX(), self.getY(), 2);
                    if (tempPoint != null) {
                        if (goOnPath(self, tempPoint.getX(), tempPoint.getY(), true)) {
                            return true;
                        }
                    } else if (goOnPath(self, targetX, targetY, true)) {
                        return true;
                    }
                }
            }

            if (trueMapOfPoints[self.getX()][self.getY()] == 4 && self.getActionPoints() >= getCostMoveWithStance(self)) {
                thePoint tempPoint = findStrongCell(self, self.getX(), self.getY(), 4);
                if (tempPoint != null) {
                    if (goOnPath(self, tempPoint.getX(), tempPoint.getY(), true)) {
                        return true;
                    }
                } else if (goOnPath(self, targetX, targetY, true)) {
                    return true;
                }
            }

            if (targetTrooper != null && detectEnemyByTeam && self.getActionPoints() >= game.getStanceChangeCost() && !world.isVisible(self.getVisionRange(), self.getX(), self.getY(), self.getStance(), localTargetX, localTargetY, TrooperStance.PRONE)) {

                LinkedList<thePoint> path = lee(self, self.getX(), self.getY(), targetTrooper.getX(), targetTrooper.getY(), true);
                LinkedList<thePoint> path1 = lee(self, self.getX(), self.getY(), targetTrooper.getX(), targetTrooper.getY(), false);

                if (path != null && path.size() > 1 || path1 != null && path1.size() > 1) {

                    if (path != null && path.size() < path1.size() + 5) {

                        if (path.size() > 4 && trueMapOfPoints[path.get(1).getX()][path.get(1).getY()] > 2 && self.getActionPoints() >= 6) {
                            if (goOnPath(self, path.get(1).getX(), path.get(1).getY(), true)) {
                                return true;
                            }
                        }

                        if (path.size() > 4 && trueMapOfPoints[path.get(1).getX()][path.get(1).getY()] < 4 && self.getActionPoints() >= 6) {
                            if (goOnPath(self, path.get(1).getX(), path.get(1).getY(), true)) {
                                return true;
                            }
                        }

                        if (path.size() > 1 && trueMapOfPoints[path.get(0).getX()][path.get(0).getY()] < 4 && self.getActionPoints() >= 6) {
                            if (goOnPath(self, path.get(1).getX(), path.get(1).getY(), true)) {
                                return true;
                            }
                        }

                        if (self.getStance() != TrooperStance.PRONE) {
                            move.setAction(ActionType.LOWER_STANCE);
                            return true;
                        }

                        move.setAction(ActionType.END_TURN);
                        return true;

                    } else {

                        if (path.size() > 4 && trueMapOfPoints[path.get(1).getX()][path.get(1).getY()] > 2 && self.getActionPoints() >= 6) {
                            if (goOnPath(self, path.get(1).getX(), path.get(1).getY(), false)) {
                                return true;
                            }
                        }

                        if (path.size() > 4 && trueMapOfPoints[path.get(1).getX()][path.get(1).getY()] < 4 && self.getActionPoints() >= 6) {
                            if (goOnPath(self, path.get(1).getX(), path.get(1).getY(), false)) {
                                return true;
                            }
                        }

                        if (path.size() > 1 && trueMapOfPoints[path.get(0).getX()][path.get(0).getY()] < 4 && self.getActionPoints() >= 6) {
                            if (goOnPath(self, path.get(1).getX(), path.get(1).getY(), false)) {
                                return true;
                            }
                        }

                        if (self.getStance() != TrooperStance.PRONE) {
                            move.setAction(ActionType.LOWER_STANCE);
                            return true;
                        }

                        move.setAction(ActionType.END_TURN);
                        return true;
                    }
                }
            }

            if (listOfEnemyTroopers.size() == 0/* && !(targetX == localTargetX && targetY == localTargetY || targetX == globalTargetX && targetY == globalTargetY)*/ && !goToBonus) {
                LinkedList<thePoint> tempPath1 = lee(self, self.getX(), self.getY(), targetX, targetY, true);
                LinkedList<thePoint> tempPath2 = lee(self, self.getX(), self.getY(), targetX, targetY, false);
                double dist = self.getDistanceTo(targetX, targetY);

                if (tempPath1 != null && tempPath1.size() > 1 && tempPath2 != null && tempPath2.size() > 1 && tempPath1.size() < tempPath2.size() + 5 && tempPath1.size() > dist + 5) {
                    thePoint point = findCloseCell(self, targetX, targetY, true);
                    if (point != null && goOnPath(self, point.getX(), point.getY(), false)) {
                        return true;
                    }
                } else if (dist > 6 && tempPath1 != null && tempPath1.size() > 1 && tempPath1.size() < tempPath2.size() + 5) {
                    thePoint point = findCloseCell(self, targetX, targetY, true);
                    if (point != null && goOnPath(self, point.getX(), point.getY(), true)) {
                        return true;
                    }
                } else if (tempPath2 != null && tempPath2.size() > 1 && tempPath2.size() > dist + 5) {
                    thePoint point = findCloseCell(self, targetX, targetY, false);
                    if (point != null && goOnPath(self, point.getX(), point.getY(), false)) {
                        return true;
                    }
                } else if (dist > 6 && tempPath2 != null && tempPath2.size() > 1) {
                    thePoint point = findCloseCell(self, targetX, targetY, false);
                    if (point != null && goOnPath(self, point.getX(), point.getY(), false)) {
                        return true;
                    }
                } else {
                    for (Trooper trooper : troopers) {
                        if (trooper.isTeammate() && trooper.getX() == targetX && trooper.getY() == targetY && self.getActionPoints() >= 4) {
                            if (goOnPath(self, targetX, targetY, false)) {
                                return true;
                            }
                        }
                    }
                }
            }

        }

        return false;
    }

    private thePoint findClosestPointForEnemyTroopers(Trooper self, Trooper target) {
        int W = world.getWidth();
        int H = world.getHeight();
        int WALL = -1;                // непроходимая ячейка
        int BLANK = -2;                // свободная непомеченная ячейка

        int[][] cellsIntTemp = new int[W][];
        for (int i = 0; i < W; i++) {
            cellsIntTemp[i] = Arrays.copyOf(cellsInt[i], cellsInt[i].length);
        }

        int goUpStance = 0;
        int actionPoints = self.getActionPoints();

        if (self.getStance() == TrooperStance.PRONE) {
            goUpStance = 4;
        } else if (self.getStance() == TrooperStance.KNEELING) {
            goUpStance = 2;
        } else if (self.getStance() == TrooperStance.STANDING) {
            goUpStance = 0;
        }
        actionPoints -= goUpStance;

        int moveLen = actionPoints / game.getStandingMoveCost() + 1; //+1 для расчёта ниже tempPath.size() - 1 < moveLen, чтобы не ставить <=
        double targetDistance = 50;

        int x1 = -1;
        int y1 = -1;

        for(Trooper trooper : listOfEnemyTroopers) {
            if (!trooper.isTeammate() && trooper == target) {

                for (int k = 0; k < W; k++) {
                    for (int m = 0; m < H; m++) {
                        LinkedList<thePoint> path = lee(self, self.getX(), self.getY(), k, m, true);
                        if (cellsIntTemp[k][m] == BLANK && path != null && path.size() > 1 && getDistancePointToPoint(k, m, target.getX(), target.getY()) <= self.getShootingRange() && getDistancePointToPoint(k, m, target.getX(), target.getY()) <= targetDistance && world.isVisible(self.getShootingRange(), k, m, TrooperStance.STANDING, trooper.getX(), trooper.getY(), trooper.getStance())) {

                            for (Trooper trooper1 : troopers) {
                                if (trooper1.getX() == k && trooper1.getY() == m) {
                                    continue;
                                }
                            }

                            for (Trooper trooper1 : listOfEnemyTroopers) {
                                if (/*!world.isVisible(trooper1.getVisionRange(), trooper1.getX(), trooper1.getY(), TrooperStance.STANDING, k, m, TrooperStance.STANDING) || !world.isVisible(trooper1.getVisionRange(), trooper1.getX(), trooper1.getY(), TrooperStance.STANDING, k, m, TrooperStance.KNEELING) && moveLen - 1 >= 0 || */!world.isVisible(trooper1.getVisionRange(), trooper1.getX(), trooper1.getY(), TrooperStance.STANDING, k, m, TrooperStance.PRONE) && moveLen - 2 >= 0) {
                                    //если попали сюда, значит можно спрятаться от вражеского юнита в одной из позиций
                                } else {
                                    //если попали сюда, значит мой юнит в этой точке виден врагу и переходим к следующей
                                    continue;
                                }
                            }

                            LinkedList<thePoint> tempPath = lee(self, self.getX(), self.getY(), k, m, true);

                            if (tempPath != null && tempPath.size() > 1 && tempPath.size() - 1 <= moveLen) {
                                moveLen = tempPath.size() - 1;
                                targetDistance = getDistancePointToPoint(k, m, target.getX(), target.getY());
                                x1 = k;
                                y1 = m;
                            }
                        }
                    }
                }

            }
        }

        if (x1 != -1 && y1 != -1) {
            return new thePoint(x1, y1);
        } else {
            return null;  // безопасная ячейка не найдена
        }
    }

    //расчёт безопасной ячейки из которой можно выстрелить по одному из вражескиъ юнитов (путь не должен содержать в себе видимого для врагов отрезка, который нельзя пройти не остановившись в нём)
    private thePoint findPointForAttackEnemyTroopers(Trooper self) {
        int W = world.getWidth();
        int H = world.getHeight();
        int WALL = -1;                // непроходимая ячейка
        int BLANK = -2;                // свободная непомеченная ячейка

        int[][] cellsIntTemp = new int[W][];
        for (int i = 0; i < W; i++) {
            cellsIntTemp[i] = Arrays.copyOf(cellsInt[i], cellsInt[i].length);
        }

        int goUpStance = 0;
        int actionPoints = self.getActionPoints();

        if (self.getStance() == TrooperStance.PRONE) {
            goUpStance = 4;
        } else if (self.getStance() == TrooperStance.KNEELING) {
            goUpStance = 2;
        } else if (self.getStance() == TrooperStance.STANDING) {
            goUpStance = 0;
        }
        actionPoints -= goUpStance;

        int moveLen = actionPoints / game.getStandingMoveCost() + 1; //+1 для расчёта ниже tempPath.size() - 1 < moveLen, чтобы не ставить <=
        int moveLen1 = 50;

        int x1 = -1;
        int y1 = -1;

        for(Trooper trooper : listOfEnemyTroopers) {
            if (!trooper.isTeammate()) {
                for (int k = 0; k < W; k++) {
                    for (int m = 0; m < H; m++) {

                        LinkedList<thePoint> path = leeGoOn(self, self.getX(), self.getY(), k, m, true);
                        if (cellsIntTemp[k][m] == BLANK && path != null && path.size() > 1 && getDistancePointToPoint(k, m, trooper.getX(), trooper.getY()) <= self.getShootingRange()) {
                            if(world.isVisible(self.getShootingRange(), k, m, TrooperStance.STANDING, trooper.getX(), trooper.getY(), ((self.getType() == TrooperType.FIELD_MEDIC || self.getType() == TrooperType.COMMANDER || self.getType() == TrooperType.SCOUT) ? trooper.getStance() : TrooperStance.PRONE ))) {

                                for (Trooper trooper1 : troopers) {
                                    if (trooper1.getX() == k && trooper1.getY() == m) {
                                        continue;
                                    }
                                }

                                int test = 0;
                                for (thePoint point : path) {
                                    for (Trooper trooper1 : listOfEnemyTroopers) {
                                        if (!world.isVisible(trooper1.getVisionRange(), trooper1.getX(), trooper1.getY(), TrooperStance.STANDING, k, m, TrooperStance.PRONE)) {
                                            test = 0;
                                        } else {
                                            test++;
                                            break;
                                        }
                                        if (test > moveLen - 2) {
                                            break;
                                        }
                                    }
                                    if (test > moveLen - 2) {
                                        break;
                                    }
                                }

                                if (test > moveLen - 2) {
                                    continue;
                                }

                                for (Trooper trooper1 : listOfEnemyTroopers) {
                                    if (!world.isVisible(trooper1.getVisionRange(), trooper1.getX(), trooper1.getY(), TrooperStance.STANDING, k, m, TrooperStance.STANDING) || !world.isVisible(trooper1.getVisionRange(), trooper1.getX(), trooper1.getY(), TrooperStance.STANDING, k, m, TrooperStance.KNEELING) && moveLen - 1 >= 0 || !world.isVisible(trooper1.getVisionRange(), trooper1.getX(), trooper1.getY(), TrooperStance.STANDING, k, m, TrooperStance.PRONE) && moveLen - 2 >= 0) {
                                        //если попали сюда, значит можно спрятаться от вражеского юнита в одной из позиций
                                    } else {
                                        //если попали сюда, значит мой юнит в этой точке виден врагу и переходим к следующей
                                        continue;
                                    }
                                }

                                if (path != null && path.size() > 1 && path.size() - 1 <= moveLen1 && actionPoints - moveLen1 - 2 < 0) {
                                    moveLen1 = path.size() - 1;
                                    x1 = k;
                                    y1 = m;
                                }

                                if (path != null && path.size() > 1 && path.size() - 1 <= moveLen) {
                                    moveLen = path.size() - 1;
                                    x1 = k;
                                    y1 = m;
                                }
                            }
                        }
                    }
                }
            }
        }

        if (x1 != -1 && y1 != -1 && moveLen != 50) {
            return new thePoint(x1, y1);
        } else if (x1 != -1 && y1 != -1 && moveLen1 != 50) {
            return new thePoint(x1, y1);
        } else {
            return null;
        }
    }


    boolean conductTheWar(Trooper self) {

        if (testForFreePassage(self)) {
            return true;
        }

        if (saveOursSouls(self)) {
            return true;
        }

        if (listOfEnemyTroopers.size() != 0) {
            if (self.getActionPoints() >= self.getShootCost()) {

                Trooper choosenOne = chooseEnemyOnDistance(self, listOfEnemyTroopers);

                if (choosenOne != null) {
                    targetTrooper = choosenOne;

                    if (canSeeOrCanShoot(self, targetTrooper, false)) {

                        if (self.getActionPoints() >= self.getShootCost() + game.getStanceChangeCost() && makeValidLowerStance(self, true)) {
                            return true;
                        }

                        if (canShootOnTarget(self, targetTrooper)) {
                            shootOnTarget(self, targetTrooper);
                            return true;
                        }
                    } else {
                        int hpOfEnemy = 121;
                        Trooper target = null;
                        for (Trooper trooper : listOfEnemyTroopers) {
                            if (canShootOnTarget(self, trooper) && trooper.getHitpoints() < hpOfEnemy) {
                                hpOfEnemy = trooper.getHitpoints();
                                target = trooper;
                            }
                        }

                        if (target != null && self.getActionPoints() >= self.getShootCost()) {
                            shootOnTarget(self, target);
                            return true;
                        }

                        if (goOnPath(self, targetTrooper.getX(), targetTrooper.getY(), false)) {
                            return true;
                        }
                    }
                }
            } else {

                if (targetTrooper != null && self.getActionPoints() >= getCostMoveWithStance(self) && !canSeeOrCanShoot(self, targetTrooper, false)) {
                    if (goOnPath(self, targetTrooper.getX(), targetTrooper.getY(), false)) {
                        return true;
                    }
                }

                if (makeValidLowerStance(self, false)) {
                    return true;
                }
            }
        } else {
            beginBattle = false;
            if (indexOfMedic != -1 && self.getType() != TrooperType.FIELD_MEDIC) {
                if (goOnWar(self, troopers[indexOfMedic].getX(), troopers[indexOfMedic].getY())) {
                    return true;
                }
            } else if (goOnWar(self, localTargetX, localTargetY)) {
                return true;
            }
        }
        return false;
    }

    boolean testTail(Trooper self, LinkedList<thePoint> pathForUnit, int targetX, int targetY) {

        boolean canMoveY1, canMoveY2, canMoveX1, canMoveX2;
        CellType[][] cells = world.getCells();

        if (self.getActionPoints() >= getCostMoveWithStance(self)) {
            if (!(pathForUnit.get(1).getX() == targetX && pathForUnit.get(1).getY() == targetY)) {
                for (Trooper troop : troopers) {
                    if (troop.getX() == pathForUnit.get(1).getX() && troop.getY() == pathForUnit.get(1).getY()) {
                        if (self.getX() - pathForUnit.get(1).getX() != 0) {       //если следующий тайл по оси Х и он занят, то пробуем обойти по оси Y вверх или вниз
                            int offsetY1 = 1;
                            int offsetY2 = -1;

                            if (self.getY() + offsetY1 < world.getHeight() && self.getY() + offsetY1 >= 0) {
                                canMoveY1 = (offsetY1 != 0 && cells[self.getX()][self.getY() + offsetY1] == CellType.FREE);
                            } else {
                                canMoveY1 = false;
                            }
                            if (self.getY() + offsetY2 < world.getHeight() && self.getY() + offsetY2 >= 0) {
                                canMoveY2 = (offsetY2 != 0 && cells[self.getX()][self.getY() + offsetY2] == CellType.FREE);
                            } else {
                                canMoveY2 = false;
                            }

//                          проверка клетки куда собираемся ходить на занятость своими или чужими юнитами
                            for (Trooper troop1 : troopers) {
                                if ((self.getX() == troop1.getX()) && self.getY() + offsetY1 == troop1.getY()) {
                                    canMoveY1 = false;
                                }
                                if ((self.getX() == troop1.getX()) && self.getY() + offsetY2 == troop1.getY()) {
                                    canMoveY2 = false;
                                }
                            }

                            if (canMoveY1 && canMoveY2) {
                                if (self.getX() == lastMoveX && self.getY() + offsetY1 == lastMoveY) {
                                    move.setX(self.getX());
                                    move.setY(self.getY() + offsetY2);
                                    break;
                                } else if (self.getX() == lastMoveX && self.getY() + offsetY2 == lastMoveY) {
                                    move.setX(self.getX());
                                    move.setY(self.getY() + offsetY1);
                                    break;
                                } else {
                                    LinkedList<thePoint> path1 = lee(self, self.getX(), self.getY(), targetX, targetY, true);
                                    LinkedList<thePoint> path2 = lee(self, self.getX(), self.getY(), targetX, targetY, false);

                                    if (path1 != null && path1.size() <= path2.size() + 5) {
                                        if (lee(self, self.getX(), self.getY() + offsetY1, targetX, targetY, true).size() <= lee(self, self.getX(), self.getY() + offsetY2, targetX, targetY, true).size()) {
                                            move.setX(self.getX());
                                            move.setY(self.getY() + offsetY1);
                                            break;
                                        } else {
                                            move.setX(self.getX());
                                            move.setY(self.getY() + offsetY2);
                                            break;
                                        }
                                    } else if (path1 != null && path1.size() > path2.size()) {
                                        if (lee(self, self.getX(), self.getY() + offsetY1, targetX, targetY, false).size() <= lee(self, self.getX(), self.getY() + offsetY2, targetX, targetY, false).size()) {
                                            move.setX(self.getX());
                                            move.setY(self.getY() + offsetY1);
                                            break;
                                        } else {
                                            move.setX(self.getX());
                                            move.setY(self.getY() + offsetY2);
                                            break;
                                        }
                                    } else {
                                        return false;
                                    }
                                }
                            } else if (canMoveY1) {
                                move.setX(self.getX());
                                move.setY(self.getY() + offsetY1);
                                break;
                            } else if (canMoveY2) {
                                move.setX(self.getX());
                                move.setY(self.getY() + offsetY2);
                                break;
                            } else {
                                return false;
                            }
                        } else if (self.getY() - pathForUnit.get(1).getY() != 0) {   //если следующий тайл по оси Y и он занят, то пробуем обойти по оси X вправо или влево
                            int offsetX1 = 1;
                            int offsetX2 = -1;

                            if (self.getX() + offsetX1 < world.getWidth() && self.getX() + offsetX1 >= 0) {
                                canMoveX1 = (offsetX1 != 0 && cells[self.getX() + offsetX1][self.getY()] == CellType.FREE);
                            } else {
                                canMoveX1 = false;
                            }
                            if (self.getX() + offsetX2 < world.getWidth() && self.getX() + offsetX2 >= 0) {
                                canMoveX2 = (offsetX2 != 0 && cells[self.getX() + offsetX2][self.getY()] == CellType.FREE);
                            } else {
                                canMoveX2 = false;
                            }

                            //проверка клетки куда собираемся ходить на занятость своими или чужими юнитами
                            for (Trooper troop1 : troopers) {
                                if ((self.getX() + offsetX1 == troop1.getX()) && self.getY() == troop1.getY()) {
                                    canMoveX1 = false;
                                }
                                if ((self.getX() + offsetX2 == troop1.getX()) && self.getY() == troop1.getY()) {
                                    canMoveX2 = false;
                                }
                            }

                            if (canMoveX1 && canMoveX2) {
                                if (self.getX() + offsetX1 == lastMoveX && self.getY() == lastMoveY) {
                                    move.setX(self.getX() + offsetX2);
                                    move.setY(self.getY());
                                    break;
                                } else if (self.getX() + offsetX2 == lastMoveX && self.getY() == lastMoveY) {
                                    move.setX(self.getX() + offsetX1);
                                    move.setY(self.getY());
                                    break;
                                } else {
                                    LinkedList<thePoint> path1 = lee(self, self.getX(), self.getY(), targetX, targetY, true);
                                    LinkedList<thePoint> path2 = lee(self, self.getX(), self.getY(), targetX, targetY, false);

                                    if (path1 != null && path1.size() <= path2.size() + 5) {
                                        if (lee(self, self.getX() + offsetX1, self.getY(), targetX, targetY, true).size() <= lee(self, self.getX() + offsetX2, self.getY(), targetX, targetY, true).size()) {
                                            move.setX(self.getX() + offsetX1);
                                            move.setY(self.getY());
                                            break;
                                        } else {
                                            move.setX(self.getX() + offsetX2);
                                            move.setY(self.getY());
                                            break;
                                        }
                                    } else if (path1 != null && path1.size() > path2.size()) {
                                        if (lee(self, self.getX() + offsetX1, self.getY(), targetX, targetY, false).size() <= lee(self, self.getX() + offsetX2, self.getY(), targetX, targetY, false).size()) {
                                            move.setX(self.getX() + offsetX1);
                                            move.setY(self.getY());
                                            break;
                                        } else {
                                            move.setX(self.getX() + offsetX2);
                                            move.setY(self.getY());
                                            break;
                                        }
                                    } else {
                                        return false;
                                    }
                                }
                            } else if (canMoveX1) {
                                move.setX(self.getX() + offsetX1);
                                move.setY(self.getY());
                                break;
                            } else if (canMoveX2) {
                                move.setX(self.getX() + offsetX2);
                                move.setY(self.getY());
                                break;
                            } else {
                                //move.setAction(ActionType.END_TURN);
                                return false;
                            }
                        }
                    }
                }
                return true;
            } else {
                for (Trooper troop : troopers) {
                    if (troop.getX() == pathForUnit.get(1).getX() && troop.getY() == pathForUnit.get(1).getY()) {
                        move.setAction(ActionType.END_TURN);
                        move.setX(self.getX());
                        move.setY(self.getY());
                        return true;
                    }
                }
                return true;
            }
        }
        return false;
    }


    thePoint find1NotAchievableTail(Trooper self, boolean seeOrShoot, int additionalRange) {
        //находит ячейку недосягаемую для чужих юнитов
        if(safeStance == null && !(istroopersUnderAttack && trooperUnderAttack == self.getId())) {

            boolean positionIsSafe;
            if ((self.getType() == TrooperType.COMMANDER || self.getType() == TrooperType.SCOUT) && complatedPathOfTrooper!= null && complatedPathOfTrooper.size() >= 3) {
                positionIsSafe = true;
                for (Trooper trooper : listOfEnemyTroopers) {
                    if (world.isVisible(trooper.getVisionRange() + additionalRange, trooper.getX(), trooper.getY(), TrooperStance.STANDING, complatedPathOfTrooper.get(complatedPathOfTrooper.size() - 2).getX(), complatedPathOfTrooper.get(complatedPathOfTrooper.size() - 2).getY(), /*self.getStance()*/ TrooperStance.STANDING)) {
                        positionIsSafe = false;
                        break;
                    }
                }
                if (positionIsSafe) {
                    goToSafePlace = true;
                    safePoint = new thePoint(complatedPathOfTrooper.get(complatedPathOfTrooper.size() - 3).getX(), complatedPathOfTrooper.get(complatedPathOfTrooper.size() - 3).getY());
                    return safePoint;
                }
            }

            boolean positionIsSafe1 = true;
            boolean positionIsSafe2 = true;

            for (Trooper trooper : listOfEnemyTroopers) {

                //если положение prone, то так как уже ищем позицию, значит нас уже видно и мы не в безопасности
                if (self.getStance() == TrooperStance.PRONE) {
                    positionIsSafe2 = false;
                }

                //если положение kneeling
                if (self.getActionPoints() < 2 || self.getStance() == TrooperStance.KNEELING && world.isVisible(trooper.getVisionRange() + additionalRange, trooper.getX(), trooper.getY(), TrooperStance.STANDING, self.getX(), self.getY(), TrooperStance.PRONE)) {
                    positionIsSafe2 = false;
                }

                //если положение standing
                if(!(self.getActionPoints() >= 4 && !world.isVisible(trooper.getVisionRange() + additionalRange, trooper.getX(), trooper.getY(), TrooperStance.STANDING, self.getX(), self.getY(), TrooperStance.PRONE))) {
                    positionIsSafe2 = false;
                }

                if(!(self.getActionPoints() >= 2 && !world.isVisible(trooper.getVisionRange() + additionalRange, trooper.getX(), trooper.getY(), TrooperStance.STANDING, self.getX(), self.getY(), TrooperStance.KNEELING))) {
                    positionIsSafe1 = false;
                }
            }

            if (positionIsSafe1 || positionIsSafe2) {
                goToSafePlace = true;
                if (positionIsSafe1) {
                    safeStance = TrooperStance.KNEELING;
                } else if (positionIsSafe2) {
                    safeStance = TrooperStance.PRONE;
                }
                safePoint = new thePoint(self.getX(), self.getY());
                return safePoint;
            }
        } else if (!(istroopersUnderAttack && trooperUnderAttack == self.getId())){
            safePoint = new thePoint(self.getX(), self.getY());
            return safePoint;
        }

        boolean positionIsSafe;
        if ((self.getType() == TrooperType.COMMANDER || self.getType() == TrooperType.SCOUT) && complatedPathOfTrooper!= null && complatedPathOfTrooper.size() >= 3) {
            positionIsSafe = true;
            for (Trooper trooper : listOfEnemyTroopers) {
                if (world.isVisible(trooper.getVisionRange() + additionalRange, trooper.getX(), trooper.getY(), TrooperStance.STANDING, complatedPathOfTrooper.get(complatedPathOfTrooper.size() - 2).getX(), complatedPathOfTrooper.get(complatedPathOfTrooper.size() - 2).getY(), /*self.getStance()*/ TrooperStance.STANDING)) {
                    positionIsSafe = false;
                    break;
                }
            }
            if (positionIsSafe) {
                goToSafePlace = true;
                safePoint = new thePoint(complatedPathOfTrooper.get(complatedPathOfTrooper.size() - 2).getX(), complatedPathOfTrooper.get(complatedPathOfTrooper.size() - 2).getY());
                return safePoint;
            }
        }

        int W = world.getWidth();
        int H = world.getHeight();
        int WALL = -1;                // непроходимая ячейка
        int BLANK = -2;                // свободная непомеченная ячейка

        int[][] cellsIntTemp = new int[W][];
        for (int i = 0; i < W; i++) {
            cellsIntTemp[i] = Arrays.copyOf(cellsInt[i], cellsInt[i].length);
        }

        /*for (int k = 0; k < W; k++) {
            for (int m = 0; m < H; m++) {
                if (cellsInt[k][m] == -5) {
                    storedCell = new thePoint(k, m);
                    storedCell.worldMove = world.getMoveIndex();
                    cellsInt[k][m] = -2;
                }
            }
        }*/

        if (seeOrShoot) {
            for (Trooper trooper : listOfEnemyTroopers) {
                for (int k = 0; k < W; k++) {
                    for (int m = 0; m < H; m++) {
                        if (trooper.getDistanceTo(k, m) <= trooper.getVisionRange() + additionalRange && cellsIntTemp[k][m] != WALL && world.isVisible(trooper.getVisionRange() + additionalRange, trooper.getX(), trooper.getY(), TrooperStance.STANDING, k, m, self.getStance())) {
                            cellsIntTemp[k][m] = WALL;
                        }
                    }
                }
            }
        } else {
            for (Trooper trooper : listOfEnemyTroopers) {
                for (int k = 0; k < W; k++) {
                    for (int m = 0; m < H; m++) {
                        if (trooper.getDistanceTo(k, m) <= trooper.getShootingRange() + additionalRange && cellsIntTemp[k][m] != WALL && world.isVisible(trooper.getShootingRange() + additionalRange, trooper.getX(), trooper.getY(), TrooperStance.STANDING, k, m, self.getStance())) {
                            cellsIntTemp[k][m] = WALL;
                        }
                    }
                }
            }
        }

        int goUpStance = 0;
        int actionPoints = self.getActionPoints();

        if (self.getStance() == TrooperStance.PRONE) {
            goUpStance = 4;
        } else if (self.getStance() == TrooperStance.KNEELING) {
            goUpStance = 2;
        } else if (self.getStance() == TrooperStance.STANDING) {
            goUpStance = 0;
        }
        actionPoints -= goUpStance;

        int moveLen = actionPoints / game.getStandingMoveCost() + 1; //+1 для расчёта ниже tempPath.size() - 1 < moveLen, чтобы не ставить <=

        int x1 = -1;
        int y1 = -1;
        double distTeam = 50;
        Trooper teamTrooper = null;
        if(forwardTrooper != -1 && troopers[forwardTrooper].getType() != self.getType() && self.getDistanceTo(troopers[forwardTrooper]) <= 5) {
            teamTrooper = troopers[forwardTrooper];
        } else {
            for (Trooper trooper : troopers) {
                if (trooper.isTeammate() && self.getDistanceTo(trooper) <= 5) {
                    teamTrooper = trooper;
                    break;
                }
            }
        }

        for (int k = 0; k < W; k++) {
            for (int m = 0; m < H; m++) {
                boolean isNotStoredCell = true;
                for (thePoint point : listOfStoredCells) {
                    if (point.getX() == k && point.getY() == m) {
                        isNotStoredCell = false;
                    }
                }

                LinkedList<thePoint> path = lee(self, self.getX(), self.getY(), k, m, true);


                if (isNotStoredCell && cellsIntTemp[k][m] == BLANK && path != null && path.size() > 1 && self.getDistanceTo(teamTrooper) <= distTeam) {

                    for (Trooper trooper : troopers) {
                        if (trooper.getX() == k && trooper.getY() == m) {
                            path = null;
                            break;
                        }
                    }
                    if (path != null && path.size() - 1 <= moveLen) {
                        moveLen = path.size() - 1;
                        x1 = k;
                        y1 = m;
                    }
                }
            }
        }

        if (x1 != -1 && y1 != -1) {
            goToSafePlace = true;
            safePoint = new thePoint(x1, y1);
            return safePoint;
        } else {
            return null;  // безопасная ячейка не найдена
        }
    }

    thePoint findNotAchievableTail(Trooper self, boolean seeOrShoot, int additionalRange) {
        //находит ячейку недосягаемую для чужих юнитов
        if(safeStance == null && !(istroopersUnderAttack && trooperUnderAttack == self.getId())) {

            boolean positionIsSafe;
            if ((self.getType() == TrooperType.COMMANDER || self.getType() == TrooperType.SCOUT) && complatedPathOfTrooper!= null && complatedPathOfTrooper.size() >= 3) {
                positionIsSafe = true;
                for (Trooper trooper : listOfEnemyTroopers) {
                    if (world.isVisible(trooper.getVisionRange() + additionalRange, trooper.getX(), trooper.getY(), TrooperStance.STANDING, complatedPathOfTrooper.get(complatedPathOfTrooper.size() - 2).getX(), complatedPathOfTrooper.get(complatedPathOfTrooper.size() - 2).getY(), /*self.getStance()*/ TrooperStance.STANDING)) {
                        positionIsSafe = false;
                        break;
                    }
                }
                if (positionIsSafe) {
                    goToSafePlace = true;
                    safePoint = new thePoint(complatedPathOfTrooper.get(complatedPathOfTrooper.size() - 3).getX(), complatedPathOfTrooper.get(complatedPathOfTrooper.size() - 3).getY());
                    return safePoint;
                }
            }

            boolean positionIsSafe1 = true;
            boolean positionIsSafe2 = true;

            for (Trooper trooper : listOfEnemyTroopers) {

                //если положение prone, то так как уже ищем позицию, значит нас уже видно и мы не в безопасности
                if (self.getStance() == TrooperStance.PRONE) {
                    positionIsSafe2 = false;
                }

                //если положение kneeling
                if (self.getActionPoints() < 2 || self.getStance() == TrooperStance.KNEELING && world.isVisible(trooper.getVisionRange() + additionalRange, trooper.getX(), trooper.getY(), TrooperStance.STANDING, self.getX(), self.getY(), TrooperStance.PRONE)) {
                    positionIsSafe2 = false;
                }

                //если положение standing
                if(!(self.getActionPoints() >= 4 && !world.isVisible(trooper.getVisionRange() + additionalRange, trooper.getX(), trooper.getY(), TrooperStance.STANDING, self.getX(), self.getY(), TrooperStance.PRONE))) {
                    positionIsSafe2 = false;
                }

                if(!(self.getActionPoints() >= 2 && !world.isVisible(trooper.getVisionRange() + additionalRange, trooper.getX(), trooper.getY(), TrooperStance.STANDING, self.getX(), self.getY(), TrooperStance.KNEELING))) {
                    positionIsSafe1 = false;
                }
            }

            if (positionIsSafe1 || positionIsSafe2) {
                goToSafePlace = true;
                if (positionIsSafe1) {
                    safeStance = TrooperStance.KNEELING;
                } else if (positionIsSafe2) {
                    safeStance = TrooperStance.PRONE;
                }
                safePoint = new thePoint(self.getX(), self.getY());
                return safePoint;
            }
        } else if (!(istroopersUnderAttack && trooperUnderAttack == self.getId())){
            safePoint = new thePoint(self.getX(), self.getY());
            return safePoint;
        }

        boolean positionIsSafe;
        if ((self.getType() == TrooperType.COMMANDER || self.getType() == TrooperType.SCOUT) && complatedPathOfTrooper!= null && complatedPathOfTrooper.size() >= 3) {
            positionIsSafe = true;
            for (Trooper trooper : listOfEnemyTroopers) {
                if (world.isVisible(trooper.getVisionRange() + additionalRange, trooper.getX(), trooper.getY(), TrooperStance.STANDING, complatedPathOfTrooper.get(complatedPathOfTrooper.size() - 2).getX(), complatedPathOfTrooper.get(complatedPathOfTrooper.size() - 2).getY(), /*self.getStance()*/ TrooperStance.STANDING)) {
                    positionIsSafe = false;
                    break;
                }
            }
            if (positionIsSafe) {
                goToSafePlace = true;
                safePoint = new thePoint(complatedPathOfTrooper.get(complatedPathOfTrooper.size() - 2).getX(), complatedPathOfTrooper.get(complatedPathOfTrooper.size() - 2).getY());
                return safePoint;
            }
        }

        final int W = world.getWidth();
        final int H = world.getHeight();
        final int WALL = -1;                // непроходимая ячейка
        final int BLANK = -2;                // свободная непомеченная ячейка
        final int INVISIBLE_WALL = -9; //видна во всех стойках;
        final int INVISIBLE_WALL_LOW = -6; //не видна только в PRONE;
        final int INVISIBLE_WALL_MIDDLE = -7; //не видна только в PRONE и KNEELING;
        final int INVISIBLE_WALL_HIGH = -8; //не видна во всех стойках;

        int[][] cellsIntTemp = new int[W][];
        for (int i = 0; i < W; i++) {
            cellsIntTemp[i] = Arrays.copyOf(cellsInt[i], cellsInt[i].length);
        }

        if (seeOrShoot) {
            for (Trooper trooper : listOfEnemyTroopers) {
                for (int k = 0; k < W; k++) {
                    for (int m = 0; m < H; m++) {
                        if (trooper.getDistanceTo(k, m) <= trooper.getVisionRange() + additionalRange && cellsIntTemp[k][m] != WALL && world.isVisible(trooper.getVisionRange() + additionalRange, trooper.getX(), trooper.getY(), TrooperStance.STANDING, k, m, TrooperStance.PRONE)) {
                            cellsIntTemp[k][m] = INVISIBLE_WALL;
                            continue;
                        }
                        if (trooper.getDistanceTo(k, m) <= trooper.getVisionRange() + additionalRange && cellsIntTemp[k][m] != WALL && world.isVisible(trooper.getVisionRange() + additionalRange, trooper.getX(), trooper.getY(), TrooperStance.STANDING, k, m, TrooperStance.KNEELING)) {
                            cellsIntTemp[k][m] = INVISIBLE_WALL_LOW;
                            continue;
                        }
                        if (trooper.getDistanceTo(k, m) <= trooper.getVisionRange() + additionalRange && cellsIntTemp[k][m] != WALL && world.isVisible(trooper.getVisionRange() + additionalRange, trooper.getX(), trooper.getY(), TrooperStance.STANDING, k, m, TrooperStance.STANDING)) {
                            cellsIntTemp[k][m] = INVISIBLE_WALL_MIDDLE;
                            continue;
                        }
                    }
                }
            }
        } else {
            for (Trooper trooper : listOfEnemyTroopers) {
                for (int k = 0; k < W; k++) {
                    for (int m = 0; m < H; m++) {
                        if (trooper.getDistanceTo(k, m) <= trooper.getShootingRange() + additionalRange && cellsIntTemp[k][m] != WALL && world.isVisible(trooper.getShootingRange() + additionalRange, trooper.getX(), trooper.getY(), TrooperStance.STANDING, k, m, TrooperStance.PRONE)) {
                            cellsIntTemp[k][m] = INVISIBLE_WALL;
                            continue;
                        }
                        if (trooper.getDistanceTo(k, m) <= trooper.getShootingRange() + additionalRange && cellsIntTemp[k][m] != WALL && world.isVisible(trooper.getShootingRange() + additionalRange, trooper.getX(), trooper.getY(), TrooperStance.STANDING, k, m, TrooperStance.KNEELING)) {
                            cellsIntTemp[k][m] = INVISIBLE_WALL_LOW;
                            continue;
                        }
                        if (trooper.getDistanceTo(k, m) <= trooper.getShootingRange() + additionalRange && cellsIntTemp[k][m] != WALL && world.isVisible(trooper.getShootingRange() + additionalRange, trooper.getX(), trooper.getY(), TrooperStance.STANDING, k, m, TrooperStance.STANDING)) {
                            cellsIntTemp[k][m] = INVISIBLE_WALL_MIDDLE;
                            continue;
                        }
                    }
                }
            }
        }

        int goUpStance = 0;
        int actionPoints = self.getActionPoints();

        if (self.getStance() == TrooperStance.PRONE) {
            goUpStance = 4;
        } else if (self.getStance() == TrooperStance.KNEELING) {
            goUpStance = 2;
        } else if (self.getStance() == TrooperStance.STANDING) {
            goUpStance = 0;
        }
        actionPoints -= goUpStance;

        int moveLen = actionPoints / game.getStandingMoveCost() + 1; //+1 для расчёта ниже tempPath.size() - 1 < moveLen, чтобы не ставить <=

        int x1 = -1;
        int y1 = -1;
        double distTeam = 50;

        Trooper teamTrooper = null;
        if(forwardTrooper != -1 && troopers[forwardTrooper].getType() != self.getType() && self.getDistanceTo(troopers[forwardTrooper]) <= 5) {
            teamTrooper = troopers[forwardTrooper];
        } else {
            for (Trooper trooper : troopers) {
                if (trooper.isTeammate() && self.getDistanceTo(trooper) <= 5) {
                    teamTrooper = trooper;
                    break;
                }
            }
        }

        for (int k = 0; k < W; k++) {
            for (int m = 0; m < H; m++) {

                boolean isNotStoredCell = true;
                for (thePoint point : listOfStoredCells) {
                    if (point.getX() == k && point.getY() == m) {
                        isNotStoredCell = false;
                    }
                }

                LinkedList<thePoint> path = null;
                if (cellsIntTemp[k][m] != WALL || cellsIntTemp[k][m] != INVISIBLE_WALL) {
                    path = lee(self, self.getX(), self.getY(), k, m, true);
                }

                if (isNotStoredCell && path != null && path.size() > 1 && self.getDistanceTo(teamTrooper) <= distTeam && (cellsIntTemp[k][m] == BLANK || cellsIntTemp[k][m] == INVISIBLE_WALL_MIDDLE || cellsIntTemp[k][m] == INVISIBLE_WALL_LOW)) {

                    int goDownStance = 0;

                    if (cellsIntTemp[k][m] == BLANK) {
                        goDownStance = 0;
                    } else if (cellsIntTemp[k][m] == INVISIBLE_WALL_MIDDLE) {
                        goDownStance = 2;
                    } else if (cellsIntTemp[k][m] == INVISIBLE_WALL_LOW) {
                        goDownStance = 4;
                    }

                    for (Trooper trooper : troopers) {
                        if (trooper.getX() == k && trooper.getY() == m) {
                            path = null;
                            break;
                        }
                    }

                    if (path != null && path.size() - 1 <= moveLen - goDownStance / 2) {
                        moveLen = path.size() - 1;
                        x1 = k;
                        y1 = m;
                    }
                }
            }
        }

        if (x1 != -1 && y1 != -1) {
            goToSafePlace = true;
            safePoint = new thePoint(x1, y1);
            return safePoint;
        } else {
            return null;  // безопасная ячейка не найдена
        }
    }

    Trooper chooseEnemyOnStatus(Trooper self, LinkedList<Trooper> listOfEnemy) {
        //выбирает энеми из списка, если их несклько и возвращает приоритетного, первого солдата, потом коммандира, потом медика.
        int medicNotDead = 0;
        Trooper medicNotDeadTrooper = null;
        int commanderNotDead = 0;
        Trooper commanderNotDeadTrooper = null;
        int soldierNotDead = 0;
        Trooper soldierNotDeadTrooper = null;
        int sniperNotDead = 0;
        Trooper sniperNotDeadTrooper = null;
        int scoutNotDead = 0;
        Trooper scoutNotDeadTrooper = null;

        for (Trooper trooper : listOfEnemy) {

            if (trooper.getType() == TrooperType.FIELD_MEDIC) {
                medicNotDead = 1;
                medicNotDeadTrooper = trooper;
            }

            if (trooper.getType() == TrooperType.COMMANDER) {
                commanderNotDead = 1;
                commanderNotDeadTrooper = trooper;
            }

            if (trooper.getType() == TrooperType.SOLDIER) {
                soldierNotDead = 1;
                soldierNotDeadTrooper = trooper;
            }

            if (trooper.getType() == TrooperType.SNIPER) {
                sniperNotDead = 1;
                sniperNotDeadTrooper = trooper;
            }

            if (trooper.getType() == TrooperType.SCOUT) {
                scoutNotDead = 1;
                scoutNotDeadTrooper = trooper;
            }
        }

        if (scoutNotDead == 1) {
            return scoutNotDeadTrooper;
        } else if (sniperNotDead == 1) {
            return sniperNotDeadTrooper;
        } else if (commanderNotDead == 1) {
            return commanderNotDeadTrooper;
        } else if (soldierNotDead == 1) {
            return soldierNotDeadTrooper;
        } else if (medicNotDead == 1) {
            return medicNotDeadTrooper;
        }

        return null;
    }

    Trooper chooseEnemyOnHP(Trooper self, LinkedList<Trooper> listOfEnemy) {

        //выбирает энеми из списка, если их несклько и возвращает приоритетного пр ХП.
        int minHP = 121;
        Trooper trooperWithMinHP = null;

        for (Trooper trooper : listOfEnemy) {
            if (trooper.getHitpoints() < minHP) {
                minHP = trooper.getHitpoints();
                trooperWithMinHP = trooper;
            }
        }

        if (trooperWithMinHP != null) {
            return trooperWithMinHP;
        }

        return null;
    }

    Trooper chooseEnemyOnDistance(Trooper self, LinkedList<Trooper> listOfEnemy) {
        //выбирает энеми из списка, если их несклько и возвращает ближнего по расстоянию.
        double minDistance = 15;
        Trooper trooperWithMinDist = null;
        boolean goChooseOnHP = false;
        boolean goChooseOnStatus = false;

        if(teamCount > 2) {
            if (listOfEnemy.size() >= 2) {

                double dist1, dist2;
                int hp1, hp2;

                for (int i = 0; i <= listOfEnemy.size() - 2; i++) {

                    dist1 = self.getDistanceTo(listOfEnemy.get(i));
                    dist2 = self.getDistanceTo(listOfEnemy.get(i + 1));
                    hp1 = listOfEnemy.get(i).getHitpoints();
                    hp2 = listOfEnemy.get(i + 1).getHitpoints();

                    if (Math.abs(dist1 - dist2) <= 2 && Math.abs(hp1 - hp2) >= 30) {
                        goChooseOnHP = true;
                        goChooseOnStatus = false;
                        break;
                    }

                    if (Math.abs(hp1 - hp2) <= 30 && Math.abs(dist1 - dist2) <= 1.5 && listOfEnemy.size() >= 2 && !goChooseOnHP) {
                        goChooseOnStatus = true;
                    }
                }
            }

            if (goChooseOnStatus) {
                Trooper goal = chooseEnemyOnStatus(self, listOfEnemy);
                if (goal != null) {
                    return goal;
                }
            }

            if (goChooseOnHP && teamCount > 2) {
                Trooper goal = chooseEnemyOnHP(self, listOfEnemy);
                if (goal != null) {
                    return goal;
                }
            }
        }

        for (Trooper trooper : listOfEnemy) {
            if (self.getDistanceTo(trooper) < minDistance) {
                minDistance = self.getDistanceTo(trooper);
                trooperWithMinDist = trooper;
            }
        }

        if (trooperWithMinDist != null) {
            return trooperWithMinDist;
        }

        return null;
    }

    boolean canShootOnTarget(Trooper self, Trooper target) {
        // возвращает true, если враг досягаем и по нему можно стрелять, иначе false
        if (self.getActionPoints() >= self.getShootCost() && canSeeOrCanShoot(self, target, false)) {
            return true;
        }
        return false;
    }

    LinkedList<thePoint> lee(Trooper self, int ax, int ay, int bx, int by, boolean isWithTroopers) {    // поиск пути из ячейки (ax, ay) в ячейку (bx, by)
        int W = world.getWidth();
        int H = world.getHeight();
        int WALL = -1;                // непроходимая ячейка
        int BLANK = -2;                // свободная непомеченная ячейка

        int[][] cellsIntTemp = new int[W][];
        for (int i = 0; i < W; i++) {
            cellsIntTemp[i] = Arrays.copyOf(cellsInt[i], cellsInt[i].length);
        }

        for (int i = 0; i < W; i++) {
            for (int j = 0; j < H; j++) {
                if (cellsInt[i][j] == -5) {
                    cellsIntTemp[i][j] = WALL;
                    cellsInt[i][j] = BLANK;
                }
            }
        }

        LinkedList<thePoint> pathTemp = new LinkedList<>();

        int len;                        // длина пути

        int[] dx = {1, 0, -1, 0};           //смещения, соответствующие соседям ячейки
        int[] dy = {0, 1, 0, -1};           //справа, снизу, слева и сверху

        int d, x, y, i;
        boolean stop;

        for (int i1 = 3; i1 >= 0; i1--) {
            int j1 = (int) (Math.random() * 4);
            int temp = dx[i1];
            dx[i1] = dx[j1];
            dx[j1] = temp;
            temp = dy[i1];
            dy[i1] = dy[j1];
            dy[j1] = temp;
        }

        if (isWithTroopers) {
            for (Trooper troop : troopers) {
                if (!(troop.getX() == bx && troop.getY() == by)) {
                    if (!(self.getType() == troop.getType() && troop.isTeammate())) {
                        cellsIntTemp[troop.getX()][troop.getY()] = WALL;
                    }
                } else {
                    if (goToSafePlace == true) {
                        return null;
                    }
                }
            }
        }

        // распространение волны
        d = 0;
        cellsIntTemp[ax][ay] = 0;               // стартовая ячейка помечена 0
        do {
            stop = true;                        // предполагаем, что все свободные клетки уже помечены
            for (x = 0; x < W; ++x) {
                for (y = 0; y < H; ++y) {
                    if (cellsIntTemp[x][y] == d) {                                                 // ячейка (x, y) помечена числом d
                        for (i = 0; i < 4; ++i) {                                                  // проходим по всем непомеченным соседям
                            if (x + dx[i] >= 0 && x + dx[i] < W && y + dy[i] >= 0 && y + dy[i] < H) {
                                if (cellsIntTemp[x + dx[i]][y + dy[i]] == BLANK) {
                                    stop = false;                                      // найдены непомеченные клетки
                                    cellsIntTemp[x + dx[i]][y + dy[i]] = d + 1;        // распространяем волну
                                }
                            } else {
                                stop = false;
                            }
                        }
                    }
                }
            }
            d++;
        } while (!stop && cellsIntTemp[bx][by] == BLANK);

        /*System.out.println("--------------------cellsIntTemp---------------------------");
        for (int j = 0; j < H; j++) {
            for (int k = 0; k < W; k++) {
                if(cellsIntTemp[k][j] >= 0 && cellsIntTemp[k][j] < 10) {
                    System.out.print(" " + cellsIntTemp[k][j] + " ");
                } else {
                    System.out.print(cellsIntTemp[k][j] + " ");
                }
            }
            System.out.println();
            for (int m = 0; m < 3; m++) {
                System.out.print("");
            }
        }*/

        if (cellsIntTemp[bx][by] == BLANK) {
            return null;  // путь не найден
        }

        // восстановление пути
        len = cellsIntTemp[bx][by];              // длина кратчайшего пути из (ax, ay) в (bx, by)
        x = bx;
        y = by;
        d = len;

        while (d > 0) {
            int costCell = 0;
            pathTemp.addFirst(new thePoint(x, y));                   // записываем ячейку (x, y) в путь
            d--;
            int x1 = -1, y1 = -1;
            for (i = 0; i < 4; ++i) {
                if (x + dx[i] >= 0 && x + dx[i] < W && y + dy[i] >= 0 && y + dy[i] < H) {
                    if (cellsIntTemp[x + dx[i]][y + dy[i]] == d && trueMapOfPoints[x + dx[i]][y + dy[i]] >= costCell) {
                        x1 = x + dx[i];
                        y1 = y + dy[i];                          // переходим в ячейку, которая на 1 ближе к старту
                        costCell = trueMapOfPoints[x][y];
                    }
                }
            }
            x = x1;
            y = y1;
        }

        pathTemp.addFirst(new thePoint(ax, ay));                  // теперь px[0..len] и py[0..len] - координаты ячеек пути

        return pathTemp;
    }

    LinkedList<thePoint> leeGoOn(Trooper self, int ax, int ay, int bx, int by, boolean isWithTroopers) {    // поиск пути из ячейки (ax, ay) в ячейку (bx, by)
        int W = world.getWidth();
        int H = world.getHeight();
        int WALL = -1;                // непроходимая ячейка
        int BLANK = -2;                // свободная непомеченная ячейка

        int[][] cellsIntTemp = new int[W][];
        for (int i = 0; i < W; i++) {
            cellsIntTemp[i] = Arrays.copyOf(cellsInt[i], cellsInt[i].length);
        }

        LinkedList<thePoint> pathTemp = new LinkedList<>();

        int len;                        // длина пути

        int[] dx = {1, 0, -1, 0};           //смещения, соответствующие соседям ячейки
        int[] dy = {0, 1, 0, -1};           //справа, снизу, слева и сверху

        int d, x, y, i;
        boolean stop;

        for (int i1 = 3; i1 >= 0; i1--) {
            int j1 = (int) (Math.random() * 4);
            int temp = dx[i1];
            dx[i1] = dx[j1];
            dx[j1] = temp;
            temp = dy[i1];
            dy[i1] = dy[j1];
            dy[j1] = temp;
        }

        if (isWithTroopers) {
            for (Trooper troop : troopers) {
                if (troop.getX() == bx && troop.getY() == by) {
                    return null;
                } else {
                    if (!(self.getType() == troop.getType() && troop.isTeammate())) {
                        cellsIntTemp[troop.getX()][troop.getY()] = WALL;
                    }
                }
            }
        }

        // распространение волны
        d = 0;
        cellsIntTemp[ax][ay] = 0;               // стартовая ячейка помечена 0
        do {
            stop = true;                        // предполагаем, что все свободные клетки уже помечены
            for (x = 0; x < W; ++x) {
                for (y = 0; y < H; ++y) {
                    if (cellsIntTemp[x][y] == d) {                                                 // ячейка (x, y) помечена числом d
                        for (i = 0; i < 4; ++i) {                                                  // проходим по всем непомеченным соседям
                            if (x + dx[i] >= 0 && x + dx[i] < W && y + dy[i] >= 0 && y + dy[i] < H) {
                                if (cellsIntTemp[x + dx[i]][y + dy[i]] == BLANK) {
                                    stop = false;                                      // найдены непомеченные клетки
                                    cellsIntTemp[x + dx[i]][y + dy[i]] = d + 1;        // распространяем волну
                                }
                            } else {
                                stop = false;
                            }
                        }
                    }
                }
            }
            d++;
        } while (!stop && cellsIntTemp[bx][by] == BLANK);

        if (cellsIntTemp[bx][by] == BLANK) {
            return null;  // путь не найден
        }

        // восстановление пути
        len = cellsIntTemp[bx][by];              // длина кратчайшего пути из (ax, ay) в (bx, by)
        x = bx;
        y = by;
        d = len;

        while (d > 0) {
            int costCell = 0;
            pathTemp.addFirst(new thePoint(x, y));                   // записываем ячейку (x, y) в путь
            d--;
            int x1 = -1, y1 = -1;
            for (i = 0; i < 4; ++i) {
                if (x + dx[i] >= 0 && x + dx[i] < W && y + dy[i] >= 0 && y + dy[i] < H) {
                    if (cellsIntTemp[x + dx[i]][y + dy[i]] == d && trueMapOfPoints[x + dx[i]][y + dy[i]] >= costCell) {
                        x1 = x + dx[i];
                        y1 = y + dy[i];                          // переходим в ячейку, которая на 1 ближе к старту
                        costCell = trueMapOfPoints[x][y];
                    }
                }
            }
            x = x1;
            y = y1;
        }

        pathTemp.addFirst(new thePoint(ax, ay));                  // теперь px[0..len] и py[0..len] - координаты ячеек пути

        if(pathTemp != null && pathTemp.size() > 1) {
            return pathTemp;
        } else {
            return null;
        }
    }

    boolean isDistanceEqualOrLessOneTail(Trooper self, Trooper target) {
        return self.getDistanceTo(target) <= 1;
    }

    boolean goHeal(Trooper self) {
        boolean needHeal;
        boolean needHealTarget = false;

        if (targetTrooper != null && self.getHitpoints() < 65 || istroopersUnderAttack && self.getId() == trooperUnderAttack) {
            boolean safePlace = true;
            for (Trooper trooper : listOfEnemyTroopers) {
                if (world.isVisible(trooper.getVisionRange() + 2, trooper.getX(), trooper.getY(), TrooperStance.STANDING, self.getX(), self.getY(), self.getStance()) && trooper.getType() != TrooperType.SCOUT || world.isVisible(trooper.getShootingRange(), trooper.getX(), trooper.getY(), TrooperStance.STANDING, self.getX(), self.getY(), self.getStance()) && trooper.getType() != TrooperType.SNIPER) {
                    safePlace = false;
                    break;
                }
            }
            if (safePlace) {
                targetHeal = self;
            } else {

                thePoint tempPoint;
                thePoint tempPoint1 = null;
                thePoint tempPoint2 = null;

                thePoint tempPoint3 = findNotAchievableTail(self, true, 2);

                if (tempPoint3 != null) {
                    tempPoint = tempPoint3;
                } else {

                    tempPoint1 = findNotAchievableTail(self, true, 0);
                    tempPoint2 = findNotAchievableTail(self, false, 0);

                    if (tempPoint1 != null && self.getX() == tempPoint1.getX() && self.getY() == tempPoint1.getY()) {
                        if (self.getActionPoints() >= game.getStanceChangeCost()) {
                            move.setAction(ActionType.LOWER_STANCE);
                            return true;
                        }
                    }

                    if (tempPoint1 == null) {
                        tempPoint = tempPoint2;
                    } else if (tempPoint2 == null) {
                        tempPoint = tempPoint1;
                    } else if (self.getDistanceTo(tempPoint1.getX(), tempPoint1.getY()) <= self.getDistanceTo(tempPoint2.getX(), tempPoint2.getY())) {
                        tempPoint = tempPoint2;
                    } else {
                        tempPoint = tempPoint1;
                    }

                }

                if (tempPoint != null) {
                    LinkedList<thePoint> tempPath1 = lee(self, self.getX(), self.getY(), tempPoint.getX(), tempPoint.getY(), true);
                    LinkedList<thePoint> tempPath2 = lee(self, self.getX(), self.getY(), tempPoint.getX(), tempPoint.getY(), false);
                    if (tempPath1 != null && tempPath1.size() > 1 && self.getActionPoints() >= (tempPath1.size() - 1) * getCostMoveWithStance(self)/* && (tempPath1.size() >= self.getDistanceTo(tempPoint.getX(), tempPoint.getY()) + 7)*/) {
                        if (goOnPath(self, tempPoint.getX(), tempPoint.getY(), true)) {
                            return true;
                        }
                    } else if (tempPath2 != null && tempPath2.size() > 1 && self.getActionPoints() >= (tempPath2.size() - 1) * getCostMoveWithStance(self)) {
                        if (goOnPath(self, tempPoint.getX(), tempPoint.getY(), false)) {
                            return true;
                        }
                    }
                }
            }
        }

        if (!(indexOfCommander == -1 && indexOfSniper == -1 && indexOfSoldier == -1 && indexOfScout == -1) || targetTrooper == null) {

            if (self.getActionPoints() > 0) {

                boolean test = false;

                if (targetHeal != null) {

                    LinkedList<thePoint> tempPath1 = lee(self, self.getX(), self.getY(), targetHeal.getX(), targetHeal.getY(), true);

                    if (tempPath1 != null && tempPath1.size() > 1 && self.getActionPoints() < (tempPath1.size() - 2) * game.getStandingMoveCost()) {

                        double len = 50;
                        needHeal = false;

                        for (Trooper trooper : troopers) {
                            needHeal = trooper.isTeammate() && trooper.getHitpoints() < HP_WHEN_HEAL && self.getDistanceTo(trooper) < len;
                            if (needHeal) {
                                len = self.getDistanceTo(trooper);
                                targetHeal = trooper;
                            }
                        }
                    }
                }

                for (Trooper troop : troopers) {
                    if (troop.isTeammate() && targetHeal != null && targetHeal.getId() == troop.getId() && troop.getHitpoints() < HP_WHEN_HEAL) {
                        targetHeal = troop;
                        needHealTarget = true;
                        test = true;
                        break;
                    }
                }

                if (test == false) {
                    targetHeal = null;
                    needHealTarget = false;
                }

                if (needHealTarget) {
                    if (targetHeal != null && self.getDistanceTo(targetHeal) <= 1) {
                        if (targetHeal.getId() == self.getId() && targetTrooper != null && self.getHitpoints() < 65) {
                            boolean safePlace = true;
                            for (Trooper trooper : listOfEnemyTroopers) {
                                if (canSeeOrCanShoot(trooper, self, false) || canSeeOrCanShoot(trooper, self, true)) {
                                    safePlace = false;
                                    break;
                                }
                            }
                            if (safePlace) {
                                targetHeal = self;
                            } else {
                                thePoint tempPoint1 = findNotAchievableTail(self, true, 0);
                                thePoint tempPoint2 = findNotAchievableTail(self, false, 0);
                                thePoint tempPoint;

                                if (tempPoint1 != null && self.getX() == tempPoint1.getX() && self.getY() == tempPoint1.getY()) {
                                    if(self.getActionPoints() >= game.getStanceChangeCost()) {
                                        move.setAction(ActionType.LOWER_STANCE);
                                        return true;
                                    }
                                }

                                if (tempPoint1 == null) {
                                    tempPoint = tempPoint2;
                                } else if (tempPoint2 == null) {
                                    tempPoint = tempPoint1;
                                } else if (self.getDistanceTo(tempPoint1.getX(), tempPoint1.getY()) <= self.getDistanceTo(tempPoint2.getX(), tempPoint2.getY())) {
                                    tempPoint = tempPoint2;
                                } else {
                                    tempPoint = tempPoint1;
                                }

                                if (tempPoint != null) {
                                    LinkedList<thePoint> tempPath1 = lee(self, self.getX(), self.getY(), tempPoint.getX(), tempPoint.getY(), true);
                                    LinkedList<thePoint> tempPath2 = lee(self, self.getX(), self.getY(), tempPoint.getX(), tempPoint.getY(), false);
                                    if (tempPath1 != null && tempPath1.size() > 1 && tempPath1.size() >= self.getDistanceTo(tempPoint.getX(), tempPoint.getY()) + 7 && self.getActionPoints() >= (tempPath1.size() - 1) * getCostMoveWithStance(self)) {
                                        if (goOnPath(self, tempPoint.getX(), tempPoint.getY(), true)) {
                                            return true;
                                        }
                                    } else if (tempPath2 != null && tempPath2.size() > 1 && self.getActionPoints() >= (tempPath2.size() - 1) * getCostMoveWithStance(self)) {
                                        if (goOnPath(self, tempPoint.getX(), tempPoint.getY(), false)) {
                                            return true;
                                        }
                                    }
                                }
                            }
                        }

                        //здесь приседание медика возле targetHeal
                        boolean isVisibleForEnemy = false;
                        for (Trooper trooper : listOfEnemyTroopers) {
                            if (world.isVisible(trooper.getVisionRange(), trooper.getX(), trooper.getY(), TrooperStance.STANDING, self.getX(), self.getY(), self.getStance()) || trooper.getType() !=TrooperType.SNIPER && world.isVisible(trooper.getShootingRange(), trooper.getX(), trooper.getY(), TrooperStance.STANDING, self.getX(), self.getY(), self.getStance())) {
                                isVisibleForEnemy = true;
                                break;
                            }
                        }

                        if (!isVisibleForEnemy) {
                            if (self.getStance() != targetHeal.getStance()) {
                                isVisibleForEnemy = true;
                            }
                        }

                        if (/*safePoint == null && */!isVisibleForEnemy && self.getStance() != targetHeal.getStance() && self.getActionPoints() >= game.getStanceChangeCost()) {
                            move.setAction(ActionType.LOWER_STANCE);
                            return true;
                        } else {

/*                            thePoint point = findNotAchievableTail(self, true, 0);

                            if (point != null) {

                                LinkedList<thePoint> tempPath = lee(self, self.getX(), self.getY(), point.getX(), point.getY(), true);
                                if (tempPath != null && tempPath.size() > 1 && self.getActionPoints() >= 2) {
                                    if (goOnPath(self, point.getX(), point.getY(), true)) {
                                        return true;
                                    }
                                }

                            }*/

                            if (self.getActionPoints() >= game.getStanceChangeCost() && self.getStance() != TrooperStance.PRONE) {
                                move.setAction(ActionType.LOWER_STANCE);
                                return true;
                            }
                        }

                        heal(self, targetHeal);
                        return true;
                    } else {
                        boolean canMove = true;
                        LinkedList<thePoint> tempPath = lee(self, self.getX(), self.getY(), targetHeal.getX(), targetHeal.getY(), true);
                        int actionPoints = self.getActionPoints();
                        int goUpStance = 0;

                        if (self.getStance() == TrooperStance.PRONE) {
                            goUpStance = 4;
                        } else if (self.getStance() == TrooperStance.KNEELING) {
                            goUpStance = 2;
                        } else if (self.getStance() == TrooperStance.STANDING) {
                            goUpStance = 0;
                        }

                        actionPoints -= goUpStance;

                        for (Trooper trooper : listOfEnemyTroopers) {

                            if (self.getDistanceTo(trooper) <= self.getVisionRange() && tempPath != null && tempPath.size() > 1 && (tempPath.size() - 1) * game.getStandingMoveCost() > actionPoints) {

                                if ( !(actionPoints >= game.getStandingMoveCost() && !world.isVisible(trooper.getVisionRange(), trooper.getX(), trooper.getY(), TrooperStance.STANDING, tempPath.get(actionPoints / 2).getX(), tempPath.get(actionPoints / 2).getY(), TrooperStance.STANDING) || actionPoints - 2 >= game.getStandingMoveCost() && !world.isVisible(trooper.getVisionRange(), trooper.getX(), trooper.getY(), TrooperStance.STANDING, tempPath.get((actionPoints - 2) / 2).getX(), tempPath.get((actionPoints - 2) / 2).getY(), TrooperStance.KNEELING) || actionPoints - 4 >= game.getStandingMoveCost() && !world.isVisible(trooper.getVisionRange(), trooper.getX(), trooper.getY(), TrooperStance.STANDING, tempPath.get((actionPoints - 4) / 2).getX(), tempPath.get((actionPoints - 4) / 2).getY(), TrooperStance.PRONE))) {
                                    canMove = false;
                                    break;
                                }
                            }
                        }

                        if (self.getActionPoints() <= 5) {
                            for (Trooper trooper : listOfEnemyTroopers) {
                                if (world.isVisible(trooper.getVisionRange(), trooper.getX(), trooper.getY(), TrooperStance.STANDING, self.getX(), self.getY(), self.getStance()) || trooper.getType() !=TrooperType.SNIPER && world.isVisible(trooper.getShootingRange(), trooper.getX(), trooper.getY(), TrooperStance.STANDING, self.getX(), self.getY(), self.getStance())) {
                                    if (!world.isVisible(trooper.getVisionRange(), trooper.getX(), trooper.getY(), TrooperStance.STANDING, self.getX(), self.getY(), TrooperStance.PRONE)) {
                                        if (self.getActionPoints() >= game.getStanceChangeCost() && self.getStance() != TrooperStance.PRONE) {
                                            move.setAction(ActionType.LOWER_STANCE);
                                            return true;
                                        } else {
                                            move.setAction(ActionType.END_TURN);
                                            return true;
                                        }
                                    }
                                }
                            }
                        }

                        if (targetHeal != null && self.getActionPoints() >= getCostMoveWithStance(self) && !(self.getDistanceTo(targetHeal) <= 1) && canMove) {
                            LinkedList<thePoint> path1 = lee(self, self.getX(), self.getY(), targetHeal.getX(), targetHeal.getY(), true);
                            LinkedList<thePoint> path2 = lee(self, self.getX(), self.getY(), targetHeal.getX(), targetHeal.getY(), false);

                            if(self.getActionPoints() < 6/* && self.getDistanceTo(targetHeal) < 5*/) {
                                if(safeStance == null) {
                                    if (clearSelfArea(self, 4, 2)) {
                                        //ничего не делаем
                                    } else if (clearSelfArea(self, 4, 3)) {
                                        if (self.getStance() != TrooperStance.KNEELING) {
                                            if (self.getStance() == TrooperStance.STANDING) {
                                                move.setAction(ActionType.LOWER_STANCE);
                                                return true;
                                            }
                                            if (self.getStance() == TrooperStance.PRONE) {
                                                move.setAction(ActionType.RAISE_STANCE);
                                                return true;
                                            }
                                        }
                                    } else {
                                        if (self.getStance() != TrooperStance.PRONE) {
                                            move.setAction(ActionType.LOWER_STANCE);
                                            return true;
                                        }
                                    }
                                } else {
                                    if (safeStance != self.getStance() || self.getStance() != TrooperStance.PRONE) {
                                        move.setAction(ActionType.LOWER_STANCE);
                                        return true;
                                    }
                                }

                                move.setAction(ActionType.END_TURN);
                                return true;
                            }

                            if (path1 != null && path1.size() <= path2.size() + 4) {
                                if (goOnPath(self, targetHeal.getX(), targetHeal.getY(), true)) {
                                    return true;
                                }
                            } else if (goOnPath(self, targetHeal.getX(), targetHeal.getY(), false)) {
                                return true;
                            }
                        }
                    }
                } else {
                    double len = 50;
                    needHeal = false;

                    for (Trooper trooper : troopers) {
                        needHeal = trooper.isTeammate() && trooper.getHitpoints() < HP_WHEN_HEAL && self.getDistanceTo(trooper) < len;
                        if (needHeal) {
                            len = self.getDistanceTo(trooper);
                            targetHeal = trooper;
                        }
                    }

                    if (targetHeal != null) {
                        needHeal = true;
                    }

                    if (needHeal) {
                        Trooper trooper1 = null;
                        if (self.getDistanceTo(targetHeal) <= 1) {
                            if (targetHeal.getId() == self.getId() && targetTrooper != null && self.getHitpoints() < 76) {
                                boolean safePlace = true;
                                for (Trooper trooper2 : listOfEnemyTroopers) {
                                    if (canSeeOrCanShoot(trooper2, self, false) || canSeeOrCanShoot(trooper2, self, true)) {
                                        safePlace = false;
                                        break;
                                    }
                                }
                                if (safePlace) {
                                    targetHeal = self;
                                } else {
                                    thePoint tempPoint1 = findNotAchievableTail(self, true, 0);
                                    thePoint tempPoint2 = findNotAchievableTail(self, false, 0);
                                    thePoint tempPoint;

                                    if (tempPoint1 != null && self.getX() == tempPoint1.getX() && self.getY() == tempPoint1.getY()) {
                                        if (self.getActionPoints() >= game.getStanceChangeCost()) {
                                            move.setAction(ActionType.LOWER_STANCE);
                                            return true;
                                        }
                                    }

                                    if (tempPoint1 == null) {
                                        tempPoint = tempPoint2;
                                        safePoint = tempPoint;
                                    } else if (tempPoint2 == null) {
                                        tempPoint = tempPoint1;
                                        safePoint = tempPoint;
                                    } else if (self.getDistanceTo(tempPoint1.getX(), tempPoint1.getY()) <= self.getDistanceTo(tempPoint2.getX(), tempPoint2.getY())) {
                                        tempPoint = tempPoint2;
                                        safePoint = tempPoint;
                                    } else {
                                        tempPoint = tempPoint1;
                                        safePoint = tempPoint;
                                    }

                                    if (tempPoint != null) {
                                        LinkedList<thePoint> tempPath1 = lee(self, self.getX(), self.getY(), tempPoint.getX(), tempPoint.getY(), true);
                                        LinkedList<thePoint> tempPath2 = lee(self, self.getX(), self.getY(), tempPoint.getX(), tempPoint.getY(), false);
                                        if (tempPath1 != null && tempPath1.size() > 1 && tempPath1.size() >= /*tempPath2.size() + 5*/self.getDistanceTo(tempPoint.getX(), tempPoint.getY()) + 7 && self.getActionPoints() >= (tempPath1.size() - 1) * getCostMoveWithStance(self)) {
                                            if (goOnPath(self, tempPoint.getX(), tempPoint.getY(), true)) {
                                                return true;
                                            }
                                        } else if (tempPath2 != null && tempPath2.size() > 1 && self.getActionPoints() >= (tempPath2.size() - 1) * getCostMoveWithStance(self)) {
                                            if (goOnPath(self, tempPoint.getX(), tempPoint.getY(), false)) {
                                                return true;
                                            }
                                        }
                                    }
                                }
                            }

                            if (targetTrooper != null && targetHeal.getHitpoints() > 75 && targetTrooper.getType() != TrooperType.SNIPER || targetTrooper != null && targetHeal.getHitpoints() > 95 && targetTrooper.getType() == TrooperType.SNIPER) {
                                if (targetTrooper != null && canShootOnTarget(self, targetTrooper)) {
                                    shootOnTarget(self, targetTrooper);
                                    return true;
                                }
                                for (Trooper trooper2 : listOfEnemyTroopers) {
                                    if (canShootOnTarget(self, trooper2)) {
                                        shootOnTarget(self, targetTrooper);
                                        return true;
                                    }
                                }
                            }

                            heal(self, targetHeal);
                            return true;
                        } else {
                            boolean canMove = true;
                            for (Trooper troop : troopers) {
                                LinkedList<thePoint> tempPath = lee(self, self.getX(), self.getY(), troop.getX(), troop.getY(), true);
                                if (!troop.isTeammate() && self.getDistanceTo(troop) <= self.getVisionRange() && tempPath != null && tempPath.size() > 1 && tempPath.size() - 1 >= self.getDistanceTo(troop) + 5) {
                                    canMove = false;
                                    trooper1 = troop;
                                    break;
                                }
                            }
                            if (self.getActionPoints() >= getCostMoveWithStance(self) && !isDistanceEqualOrLessOneTail(self, targetHeal) && canMove) {
                                LinkedList<thePoint> path1 = lee(self, self.getX(), self.getY(), targetHeal.getX(), targetHeal.getY(), true);
                                LinkedList<thePoint> path2 = lee(self, self.getX(), self.getY(), targetHeal.getX(), targetHeal.getY(), false);

                                if (path1 != null && path1.size() <= path2.size() + 5) {
                                    if (goOnPath(self, targetHeal.getX(), targetHeal.getY(), true)) {
                                        return true;
                                    }
                                } else if (goOnPath(self, targetHeal.getX(), targetHeal.getY(), false)) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }

        if (tryToUseMedkit(self)) {
            return true;
        }
        return false;
    }


    void heal(Trooper self, Trooper target) {
        if (self.getActionPoints() >= game.getFieldMedicHealCost() && self.getActionPoints() < 4) {
            if (tryToUseMedkit(self)) {
                useMedikit(self, target);
            }
        }
        if (self.getActionPoints() >= game.getFieldMedicHealCost()) {
            if (targetHeal != null && targetHeal.getType() == TrooperType.FIELD_MEDIC) {
                if (self.getHitpoints() + 3 > HP_WHEN_HEAL) {
                    targetHeal = null;
                }
            }
            if (targetHeal != null && targetHeal.getType() != TrooperType.FIELD_MEDIC) {
                if (self.getHitpoints() + 5 > HP_WHEN_HEAL) {
                    targetHeal = null;
                }
            }
            move.setAction(ActionType.HEAL);
            move.setX(target.getX());
            move.setY(target.getY());
        }
    }

    boolean tryToUseMedkit(Trooper self) {
        if (self.isHoldingMedikit() && self.getActionPoints() >= game.getMedikitUseCost()) {
            for (Trooper trooper : troopers) {
                if (trooper.isTeammate()) {
                    if (isDistanceEqualOrLessOneTail(self, trooper) && trooper.getHitpoints() < 60) {
                        useMedikit(self, trooper);
                        return true;
                    }
                }
            }
        }
        return false;
    }


    void useMedikit(Trooper self, Trooper trooper) {
        if (self.getActionPoints() >= ACTION_POINT_OF_MEDIKIT_USE && self.isHoldingMedikit()) {
            move.setAction(ActionType.USE_MEDIKIT);
            move.setX(trooper.getX());
            move.setY(trooper.getY());
        }
    }


    thePoint tryToUseGrenade(Trooper self, int targetX, int targetY) {
        if (self.isHoldingGrenade()) {
            return new thePoint(targetX, targetY);
        }
        return null;
    }

    void useGrenade(Trooper self, int x, int y) {
        if (self.isHoldingFieldRation() && self.getActionPoints() < ACTION_POINT_OF_GRENADE_THROW) {
            useFieldRation(self);
            return;
        }
        if (self.isHoldingGrenade() && self.getActionPoints() >= ACTION_POINT_OF_GRENADE_THROW && self.getDistanceTo(x, y) <= AREA_OF_GRENADE) {
            goThrowGrenade = false;
            move.setAction(ActionType.THROW_GRENADE);
            move.setX(x);
            move.setY(y);
            return;
        }
    }


    void useFieldRation(Trooper self) {
        if (self.getActionPoints() >= ACTION_POINT_OF_FIELD_RATION_EAT && self.isHoldingFieldRation()) {
            move.setAction(ActionType.EAT_FIELD_RATION);
            move.setX(self.getX());
            move.setY(self.getY());
        }
    }


    void randomTarget(int x, int y) {
        int maxX = world.getWidth();
        int maxY = world.getHeight();
        int quarterWhereWeAre;

        if (x < maxX / 2) {
            if (y < maxY / 2) {
                quarterWhereWeAre = 0;
            } else {
                quarterWhereWeAre = 3;
            }
        } else if (y < maxY / 2) {
            quarterWhereWeAre = 1;
        } else {
            quarterWhereWeAre = 2;
        }

        if (globalTargetX == -1 && globalTargetY == -1) {
            remainingQuarters.remove(remainingQuarters.indexOf(quarterWhereWeAre));
        }

        int temp;
        while (true) {
            temp = random.nextInt() % 4;
            if (temp != quarterWhereWeAre && remainingQuarters.contains(temp) && temp >= 0) {
                remainingQuarters.remove(remainingQuarters.indexOf(temp));
                if (remainingQuarters.size() == 0) {
                    for (int i = 0; i < 4; i++) {
                        remainingQuarters.add(i);
                    }
                    remainingQuarters.remove(remainingQuarters.indexOf(temp));
                }
                break;
            }
        }

        CellType[][] cells = world.getCells();
        switch (temp) {
            case 0: {
                int x1 = 1;
                int y1 = 1;
                boolean flag = true;
                do {
                    for (int i = 1; i <= x1; i++) {
                        if (cells[i][y1] == CellType.FREE) {
                            flag = false;
                            globalTargetX = i;
                            globalTargetY = y1;
                            break;
                        }
                    }

                    if (!flag) {
                        break;
                    }

                    for (int i = 1; i <= y1; i++) {
                        if (cells[x1][i] == CellType.FREE) {
                            flag = false;
                            globalTargetX = x1;
                            globalTargetY = i;
                            break;
                        }
                    }
                    x1++;
                    y1++;
                } while (flag && x1 < world.getWidth() / 2 && y1 < world.getHeight() / 2);
                break;
            }
            case 1: {
                int x1 = world.getWidth() - 1 - 1;
                int y1 = 1;
                boolean flag = true;
                do {
                    for (int i = world.getWidth() - 1 - 1; i >= x1; i--) {
                        if (cells[i][y1] == CellType.FREE) {
                            flag = false;
                            globalTargetX = i;
                            globalTargetY = y1;
                            break;
                        }
                    }

                    if (!flag) {
                        break;
                    }

                    for (int i = 1; i <= y1; i++) {
                        if (cells[x1][i] == CellType.FREE) {
                            flag = false;
                            globalTargetX = x1;
                            globalTargetY = i;
                            break;
                        }
                    }
                    x1--;
                    y1++;
                } while (flag && x1 >= world.getWidth() / 2 && y1 < world.getHeight() / 2);
                break;
            }
            case 2: {
                int x1 = world.getWidth() - 1 - 1;
                int y1 = world.getHeight() - 1 - 1;
                boolean flag = true;
                do {
                    for (int i = world.getWidth() - 1 - 1; i >= x1; i--) {
                        if (cells[i][y1] == CellType.FREE) {
                            flag = false;
                            globalTargetX = i;
                            globalTargetY = y1;
                            break;
                        }
                    }

                    if (!flag) {
                        break;
                    }

                    for (int i = world.getHeight() - 1 - 1; i >= y1; i--) {
                        if (cells[x1][i] == CellType.FREE) {
                            flag = false;
                            globalTargetX = x1;
                            globalTargetY = i;
                            break;
                        }
                    }
                    x1--;
                    y1--;
                } while (flag && x1 >= world.getWidth() / 2 && y1 >= world.getHeight() / 2);
                break;
            }
            case 3: {
                int x1 = 1;
                int y1 = world.getHeight() - 1 - 1;
                boolean flag = true;
                do {
                    for (int i = 1; i <= x1; i++) {
                        if (cells[i][y1] == CellType.FREE) {
                            flag = false;
                            globalTargetX = i;
                            globalTargetY = y1;
                            break;
                        }
                    }

                    if (!flag) {
                        break;
                    }

                    for (int i = world.getHeight() - 1 - 1; i >= y1; i--) {
                        if (cells[x1][i] == CellType.FREE) {
                            flag = false;
                            globalTargetX = x1;
                            globalTargetY = i;
                            break;
                        }
                    }
                    x1++;
                    y1--;
                } while (flag && x1 < world.getWidth() / 2 && y1 < world.getHeight() / 2);
                break;
            }
            default: {
                globalTargetX = world.getWidth() / 2;
                globalTargetY = world.getHeight() / 2;
            }
        }

    }

    //возвращает null при броске гранаты, свои координаты если нету точки для броска или координаты точки(-ек) куда нужно бросить гранату, если расстояние до неё(-их) больше расстояния броска гранаты
    LinkedList<thePoint> takeAPointsForGrenadeThrow(Trooper self) {

        LinkedList<thePoint> listOfGrenadePoint = new LinkedList<>();

        if (listOfEnemyTroopers.size() > 1) {
            thePoint targetPointForGrenade;
            int raznicaX;
            int raznicaY;

            for (int i = 0; i <= listOfEnemyTroopers.size() - 2; i++) {
                for (int j = i + 1; j <= listOfEnemyTroopers.size() - 1; j++) {

                    if (Math.abs(listOfEnemyTroopers.get(i).getX() - listOfEnemyTroopers.get(j).getX()) <= 2 && Math.abs(listOfEnemyTroopers.get(i).getY() - listOfEnemyTroopers.get(j).getY()) <= 2 && listOfEnemyTroopers.get(i).getDistanceTo(listOfEnemyTroopers.get(j)) <= 2) {

                        raznicaX = listOfEnemyTroopers.get(i).getX() - listOfEnemyTroopers.get(j).getX();

                        switch (raznicaX) {
                            case -2: {
                                targetPointForGrenade = tryToUseGrenade(self, listOfEnemyTroopers.get(i).getX() + 1, listOfEnemyTroopers.get(i).getY());
                                if (targetPointForGrenade != null) {
                                    listOfGrenadePoint.add(targetPointForGrenade);
                                }
                                break;
                            }
                            case -1: {
                                raznicaY = listOfEnemyTroopers.get(i).getY() - listOfEnemyTroopers.get(j).getY();
                                switch (raznicaY) {
                                    case 1: {
                                        targetPointForGrenade = tryToUseGrenade(self, listOfEnemyTroopers.get(i).getX() + 1, listOfEnemyTroopers.get(i).getY());
                                        if (targetPointForGrenade != null) {
                                            listOfGrenadePoint.add(targetPointForGrenade);
                                        }
                                        targetPointForGrenade = tryToUseGrenade(self, listOfEnemyTroopers.get(i).getX(), listOfEnemyTroopers.get(i).getY() - 1);
                                        if (targetPointForGrenade != null) {
                                            listOfGrenadePoint.add(targetPointForGrenade);
                                        }
                                        break;
                                    }
                                    case 0: {
                                        targetPointForGrenade = tryToUseGrenade(self, listOfEnemyTroopers.get(i).getX(), listOfEnemyTroopers.get(i).getY());
                                        if (targetPointForGrenade != null) {
                                            listOfGrenadePoint.add(targetPointForGrenade);
                                        }
                                        targetPointForGrenade = tryToUseGrenade(self, listOfEnemyTroopers.get(j).getX(), listOfEnemyTroopers.get(j).getY());
                                        if (targetPointForGrenade != null) {
                                            listOfGrenadePoint.add(targetPointForGrenade);
                                        }
                                        break;
                                    }
                                    case -1: {
                                        targetPointForGrenade = tryToUseGrenade(self, listOfEnemyTroopers.get(i).getX() + 1, listOfEnemyTroopers.get(i).getY());
                                        if (targetPointForGrenade != null) {
                                            listOfGrenadePoint.add(targetPointForGrenade);
                                        }
                                        targetPointForGrenade = tryToUseGrenade(self, listOfEnemyTroopers.get(i).getX(), listOfEnemyTroopers.get(i).getY() + 1);
                                        if (targetPointForGrenade != null) {
                                            listOfGrenadePoint.add(targetPointForGrenade);
                                        }
                                        break;
                                    }
                                }
                                break;
                            }
                            case 0: {
                                raznicaY = listOfEnemyTroopers.get(i).getY() - listOfEnemyTroopers.get(j).getY();
                                switch (raznicaY) {
                                    case 2: {
                                        targetPointForGrenade = tryToUseGrenade(self, listOfEnemyTroopers.get(i).getX(), listOfEnemyTroopers.get(i).getY() - 1);
                                        if (targetPointForGrenade != null) {
                                            listOfGrenadePoint.add(targetPointForGrenade);
                                        }
                                        break;
                                    }
                                    case 1: {
                                        targetPointForGrenade = tryToUseGrenade(self, listOfEnemyTroopers.get(i).getX(), listOfEnemyTroopers.get(i).getY());
                                        if (targetPointForGrenade != null) {
                                            listOfGrenadePoint.add(targetPointForGrenade);
                                        }
                                        targetPointForGrenade = tryToUseGrenade(self, listOfEnemyTroopers.get(j).getX(), listOfEnemyTroopers.get(j).getY());
                                        if (targetPointForGrenade != null) {
                                            listOfGrenadePoint.add(targetPointForGrenade);
                                        }
                                        break;
                                    }
                                    case -1: {
                                        targetPointForGrenade = tryToUseGrenade(self, listOfEnemyTroopers.get(i).getX(), listOfEnemyTroopers.get(i).getY());
                                        if (targetPointForGrenade != null) {
                                            listOfGrenadePoint.add(targetPointForGrenade);
                                        }
                                        targetPointForGrenade = tryToUseGrenade(self, listOfEnemyTroopers.get(j).getX(), listOfEnemyTroopers.get(j).getY());
                                        if (targetPointForGrenade != null) {
                                            listOfGrenadePoint.add(targetPointForGrenade);
                                        }
                                        break;
                                    }
                                    case -2: {
                                        targetPointForGrenade = tryToUseGrenade(self, listOfEnemyTroopers.get(i).getX(), listOfEnemyTroopers.get(i).getY() - 1);
                                        if (targetPointForGrenade != null) {
                                            listOfGrenadePoint.add(targetPointForGrenade);
                                        }
                                        break;
                                    }
                                }
                                break;
                            }
                            case 1: {
                                raznicaY = listOfEnemyTroopers.get(i).getY() - listOfEnemyTroopers.get(j).getY();
                                switch (raznicaY) {
                                    case 1: {
                                        targetPointForGrenade = tryToUseGrenade(self, listOfEnemyTroopers.get(i).getX() - 1, listOfEnemyTroopers.get(i).getY());
                                        if (targetPointForGrenade != null) {
                                            listOfGrenadePoint.add(targetPointForGrenade);
                                        }
                                        targetPointForGrenade = tryToUseGrenade(self, listOfEnemyTroopers.get(i).getX(), listOfEnemyTroopers.get(i).getY() - 1);
                                        if (targetPointForGrenade != null) {
                                            listOfGrenadePoint.add(targetPointForGrenade);
                                        }
                                        break;
                                    }
                                    case 0: {
                                        targetPointForGrenade = tryToUseGrenade(self, listOfEnemyTroopers.get(i).getX(), listOfEnemyTroopers.get(i).getY());
                                        if (targetPointForGrenade != null) {
                                            listOfGrenadePoint.add(targetPointForGrenade);
                                        }
                                        targetPointForGrenade = tryToUseGrenade(self, listOfEnemyTroopers.get(j).getX(), listOfEnemyTroopers.get(j).getY());
                                        if (targetPointForGrenade != null) {
                                            listOfGrenadePoint.add(targetPointForGrenade);
                                        }
                                        break;
                                    }
                                    case -1: {
                                        targetPointForGrenade = tryToUseGrenade(self, listOfEnemyTroopers.get(i).getX() - 1, listOfEnemyTroopers.get(i).getY());
                                        if (targetPointForGrenade != null) {
                                            listOfGrenadePoint.add(targetPointForGrenade);
                                        }
                                        targetPointForGrenade = tryToUseGrenade(self, listOfEnemyTroopers.get(i).getX(), listOfEnemyTroopers.get(i).getY() + 1);
                                        if (targetPointForGrenade != null) {
                                            listOfGrenadePoint.add(targetPointForGrenade);
                                        }
                                        break;
                                    }
                                }
                                break;
                            }
                            case 2: {
                                targetPointForGrenade = tryToUseGrenade(self, listOfEnemyTroopers.get(i).getX() - 1, listOfEnemyTroopers.get(i).getY());
                                if (targetPointForGrenade != null) {
                                    listOfGrenadePoint.add(targetPointForGrenade);
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }
        return listOfGrenadePoint.size() == 0 ? null : listOfGrenadePoint;
    }

    boolean makeABoom(Trooper self) {
        if (self.getActionPoints() >= ACTION_POINT_OF_GRENADE_THROW && self.isHoldingGrenade() || self.getActionPoints() >= ACTION_POINT_OF_GRENADE_THROW - 3 && self.isHoldingGrenade() && self.isHoldingFieldRation()) {

            LinkedList<thePoint> targetListOfPointForGrenade = takeAPointsForGrenadeThrow(self);
            if (targetListOfPointForGrenade != null) {
                thePoint targetPoint = new thePoint(-1, -1);
                int enemyCountNearPointOfGrenade;
                int maxEnemyNum = 0;
                double pathSize = 25;
                thePoint targetMovePoint = null;
                for (thePoint point : targetListOfPointForGrenade) {
                    int x1 = point.getX() - 1;
                    int y1 = point.getY() - 1;
                    int x2 = point.getX() + 1;
                    int y2 = point.getY() + 1;
                    enemyCountNearPointOfGrenade = 0;

                    if (x1 < 0) {
                        x1++;
                    }

                    if (y1 < 0) {
                        y1++;
                    }

                    if (x2 >= world.getWidth()) {
                        x2--;
                    }

                    if (y2 >= world.getHeight()) {
                        y2--;
                    }

                    for (int i = x1; i <= x2; i++) {
                        for (int j = y1; j <= y2; j++) {
                            if(getDistancePointToPoint(point.getX(), point.getY(), i, j) <= 2) {
                                for (Trooper trooper : listOfEnemyTroopers) {
                                    if (trooper.getX() == i && trooper.getY() == j) {
                                        enemyCountNearPointOfGrenade++;
                                    }
                                }
                            }
                        }
                    }

                    LinkedList<thePoint> targetList = new LinkedList<>();
                    targetList.add(point);
                    thePoint targetForMovePoint = goAndMakeABoom (self, targetList);

                    if(targetForMovePoint != null) {
                        if (targetForMovePoint.getX() == self.getX() && targetForMovePoint.getY() == self.getY()) {
                            if (enemyCountNearPointOfGrenade >= maxEnemyNum) {
                                targetPoint = point;
                                maxEnemyNum = enemyCountNearPointOfGrenade;
                            }
                        } else {

                            int goUpStance = 0;

                            if (self.getStance() == TrooperStance.PRONE) {
                                goUpStance = 4;
                            } else if (self.getStance() == TrooperStance.KNEELING) {
                                goUpStance = 2;
                            } else if (self.getStance() == TrooperStance.STANDING) {
                                goUpStance = 0;
                            }

                            LinkedList<thePoint> pathTemp1 = lee(self, self.getX(), self.getY(), targetForMovePoint.getX(), targetForMovePoint.getY(), true);
                            if (pathTemp1 != null && pathTemp1.size() > 1 && enemyCountNearPointOfGrenade >= maxEnemyNum && self.getActionPoints() - ACTION_POINT_OF_GRENADE_THROW + (self.isHoldingFieldRation() ? 3 : 0) >= (pathTemp1.size() - 1) * 2 + goUpStance) {
                                if (targetPoint.getX() == self.getX() && targetPoint.getY() == self.getY() && enemyCountNearPointOfGrenade > maxEnemyNum) {
                                    targetPoint = point;
                                    maxEnemyNum = enemyCountNearPointOfGrenade;
                                    targetMovePoint = targetForMovePoint;
                                } else if (enemyCountNearPointOfGrenade >= maxEnemyNum && pathTemp1.size() - 1 + goUpStance / 2 < pathSize){
                                    targetPoint = point;
                                    maxEnemyNum = enemyCountNearPointOfGrenade;
                                    pathSize = pathTemp1.size() - 1 + goUpStance / 2;
                                    targetMovePoint = targetForMovePoint;
                                }
                            }
                        }
                    }
                }

                if (targetPoint.getX() != -1) {
                    if (self.getDistanceTo(targetPoint.getX(), targetPoint.getY()) <= AREA_OF_GRENADE) {
                        useGrenade(self, targetPoint.getX(), targetPoint.getY());
                        return true;
                    }

                    if (targetMovePoint != null && self.getActionPoints() >= 2) {
                        goThrowGrenade = true;
                        if (goOnPath(self, targetMovePoint.getX(), targetMovePoint.getY(), true)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    thePoint goAndMakeABoom(Trooper self, LinkedList<thePoint> targetListOfPointForGrenade) {

        int distance = 10;
        thePoint bestPoint = new thePoint(-1, -1);
        for (thePoint point : targetListOfPointForGrenade) {

            if (self.getDistanceTo(point.getX(), point.getY()) <= AREA_OF_GRENADE) {
                return new thePoint(self.getX(), self.getY());
            }

            int actionPoints = self.getActionPoints();
            int goUpStance = 0;
            int afterUseRationFieldPoints = 3;

            if (self.getStance() == TrooperStance.PRONE) {
                goUpStance = 4;
            } else if (self.getStance() == TrooperStance.KNEELING) {
                goUpStance = 2;
            } else if (self.getStance() == TrooperStance.STANDING) {
                goUpStance = 0;
            }

            if (self.isHoldingFieldRation()) {
                actionPoints += afterUseRationFieldPoints;
            }

            if (!(self.getDistanceTo(point.getX(), point.getY()) <= AREA_OF_GRENADE)) {
                actionPoints -= goUpStance;
            }

            thePoint tempBestPoint = findShortestPathForThrowGrenade(self, point, actionPoints);
            LinkedList<thePoint> tempPath = null;
            if (tempBestPoint != null) {
                tempPath = lee(self, self.getX(), self.getY(), tempBestPoint.getX(), tempBestPoint.getY(), true);
            }

            if (tempPath != null && tempPath.size() > 1 && tempPath.size() - 1 < distance) {
                distance = tempPath.size() - 1;
                bestPoint = tempBestPoint;
            }
        }

        if (bestPoint.getX() != -1) {
            return bestPoint;
        } else {
            return null;
        }
    }

    thePoint findShortestPathForThrowGrenade(Trooper self, thePoint point, int actionPoints) {

        int W = world.getWidth();
        int H = world.getHeight();
        int WALL = -1;                // непроходимая ячейка
        int BLANK = -2;                // свободная непомеченная ячейка

        int[][] cellsIntTemp = new int[W][];
        for (int i = 0; i < W; i++) {
            cellsIntTemp[i] = Arrays.copyOf(cellsInt[i], cellsInt[i].length);
        }

        for (int i = 0; i < troopers.length; i++) {
            Trooper trooper = troopers[i];
            for (int k = 0; k < W; k++) {
                for (int m = 0; m < H; m++) {
                    if (trooper.getX() == k && trooper.getY() == m) {
                        cellsIntTemp[k][m] = WALL;
                    }
                }
            }
        }

        int moveLen = actionPoints / game.getStandingMoveCost();
        int minPathSize = 50;

        int x1 = -1;
        int y1 = -1;

        for (int k = 0; k < W; k++) {
            for (int m = 0; m < H; m++) {
                if (cellsIntTemp[k][m] == BLANK && getDistancePointToPoint(point.getX(), point.getY(), k, m) <= AREA_OF_GRENADE) {
                    LinkedList<thePoint> tempPath = lee(self, self.getX(), self.getY(), k, m, true);
                    if (tempPath != null && tempPath.size() > 1 && tempPath.size() - 1 < moveLen) {
                        if (tempPath.size() - 1 < minPathSize) {
                            minPathSize = tempPath.size() - 1;
                            x1 = k;
                            y1 = m;
                        }
                    }
                }
            }
        }

        if (x1 != -1 && y1 != -1) {
            return new thePoint(x1, y1);
        } else {
            return null;  // безопасная ячейка не найдена
        }
    }

    double getDistancePointToPoint(int x1, int y1, int x2, int y2) {
        return hypot(x1 - x2, y1 - y2);
    }

    boolean makeValidLowerStance(Trooper self, boolean inWar) {

        if (self.getActionPoints() >= game.getStanceChangeCost()) {

            if (targetTrooper != null && self.getShootingRange() >= self.getDistanceTo(targetTrooper)) {
                boolean canShoot;
                if (self.getStance() == TrooperStance.STANDING) {
                    canShoot = world.isVisible(self.getShootingRange(), self.getX(), self.getY(), TrooperStance.KNEELING, targetTrooper.getX(), targetTrooper.getY(), targetTrooper.getStance());
                } else if (self.getStance() == TrooperStance.KNEELING) {
                    canShoot = world.isVisible(self.getShootingRange(), self.getX(), self.getY(), TrooperStance.PRONE, targetTrooper.getX(), targetTrooper.getY(), targetTrooper.getStance());
                } else {
                    canShoot = false;
                }

                if (canShoot && self.getStance() != TrooperStance.PRONE && self.getDistanceTo(targetTrooper) <= self.getShootingRange() && (( (self.getActionPoints() % self.getShootCost() >= 2 && self.getActionPoints() % self.getShootCost() < self.getShootCost()) ? self.getActionPoints() >= self.getShootCost() + game.getStanceChangeCost() : false) && inWar || (self.getActionPoints() >= game.getStanceChangeCost()) && !inWar)) {
                    move.setAction(ActionType.LOWER_STANCE);
                    return true;
                }
            } else {
                if (targetTrooper != null) {
                    for (Trooper trooper : listOfEnemyTroopers) {
                        boolean canShoot;
                        if (self.getStance() == TrooperStance.STANDING) {
                            canShoot = world.isVisible(self.getShootingRange(), self.getX(), self.getY(), TrooperStance.KNEELING, trooper.getX(), trooper.getY(), trooper.getStance());
                        } else if (self.getStance() == TrooperStance.KNEELING) {
                            canShoot = world.isVisible(self.getShootingRange(), self.getX(), self.getY(), TrooperStance.PRONE, trooper.getX(), trooper.getY(), trooper.getStance());
                        } else {
                            canShoot = false;
                        }

                        if (canShoot && self.getStance() != TrooperStance.PRONE && self.getDistanceTo(trooper) <= self.getShootingRange() && (( (self.getActionPoints() % self.getShootCost() >= 2 && self.getActionPoints() % self.getShootCost() < self.getShootCost()) ? self.getActionPoints() >= self.getShootCost() + game.getStanceChangeCost() : false) && inWar || (self.getActionPoints() >= game.getStanceChangeCost()) && !inWar)) {
                            move.setAction(ActionType.LOWER_STANCE);
                            return true;
                        }
                    }
                } else {                               //TODO посмотреть для чего это
                    if (localTargetX != 100) {
                        boolean canShoot;
                        if (self.getStance() == TrooperStance.STANDING) {
                            canShoot = world.isVisible(self.getShootingRange(), self.getX(), self.getY(), TrooperStance.KNEELING, localTargetX, localTargetY, TrooperStance.STANDING);
                        } else if (self.getStance() == TrooperStance.KNEELING) {
                            canShoot = world.isVisible(self.getShootingRange(), self.getX(), self.getY(), TrooperStance.PRONE, localTargetX, localTargetY, TrooperStance.STANDING);
                        } else {
                            canShoot = false;
                        }

                        if (canShoot && self.getStance() != TrooperStance.PRONE && self.getDistanceTo(localTargetX, localTargetY) <= self.getShootingRange() + 1 && ( (self.getActionPoints() % self.getShootCost() >= 2 && self.getActionPoints() % self.getShootCost() < self.getShootCost()) ? self.getActionPoints() >= self.getShootCost() + game.getStanceChangeCost() : false)) {
                            move.setAction(ActionType.LOWER_STANCE);
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }


    void shootOnTarget(Trooper self, Trooper target) {

        boolean isInFog = false;

        for (GameUnit gameUnit : listOfSowEnemys) {
            if (target.getId() == gameUnit.trooper.getId()) {
                isInFog = !gameUnit.isNotInFog;
                break;
            }
        }

        if (isInFog) {

            isShootingAnywhere = true;
            targetUnitIdSave = (int) target.getId();

            for (Player player : world.getPlayers()) {
                if (player.getName().equalsIgnoreCase("darkstone")) {
                    myScore = player.getScore();
                    break;
                }
            }

        }

        if (self.getActionPoints() >= self.getShootCost() && self.getDistanceTo(target) <= self.getShootingRange()) {
            if(!world.isVisible(self.getVisionRange(), self.getX(), self.getY(), self.getStance(), target.getX(), target.getY(), target.getStance())) {
                for (GameUnit gameUnit : listOfSowEnemys) {
                    if (gameUnit.trooper.getId() == target.getId()) {
                        if (target.getHitpoints() > self.getDamage()) {
                            gameUnit.trooper = new Trooper(gameUnit.trooper.getId(), gameUnit.trooper.getX(), gameUnit.trooper.getY(), gameUnit.trooper.getPlayerId(), gameUnit.trooper.getTeammateIndex(), gameUnit.trooper.isTeammate(), gameUnit.trooper.getType(), gameUnit.trooper.getStance(), gameUnit.trooper.getHitpoints() - self.getDamage(), gameUnit.trooper.getMaximalHitpoints(), gameUnit.trooper.getActionPoints(), gameUnit.trooper.getInitialActionPoints(), gameUnit.trooper.getVisionRange(), gameUnit.trooper.getShootingRange(), gameUnit.trooper.getShootCost(), gameUnit.trooper.getStandingDamage(), gameUnit.trooper.getKneelingDamage(), gameUnit.trooper.getProneDamage(), gameUnit.trooper.getDamage(), gameUnit.trooper.isHoldingGrenade(), gameUnit.trooper.isHoldingMedikit(), gameUnit.trooper.isHoldingFieldRation());
                            break;
                        } else {
                            gameUnit.trooper = new Trooper(gameUnit.trooper.getId(), gameUnit.trooper.getX(), gameUnit.trooper.getY(), gameUnit.trooper.getPlayerId(), gameUnit.trooper.getTeammateIndex(), gameUnit.trooper.isTeammate(), gameUnit.trooper.getType(), gameUnit.trooper.getStance(), 100, gameUnit.trooper.getMaximalHitpoints(), gameUnit.trooper.getActionPoints(), gameUnit.trooper.getInitialActionPoints(), gameUnit.trooper.getVisionRange(), gameUnit.trooper.getShootingRange(), gameUnit.trooper.getShootCost(), gameUnit.trooper.getStandingDamage(), gameUnit.trooper.getKneelingDamage(), gameUnit.trooper.getProneDamage(), gameUnit.trooper.getDamage(), gameUnit.trooper.isHoldingGrenade(), gameUnit.trooper.isHoldingMedikit(), gameUnit.trooper.isHoldingFieldRation());
                            break;
                        }
                    }
                }
            } else {
                for (GameUnit gameUnit : listOfSowEnemys) {
                    if (gameUnit.trooper.getId() == target.getId()) {
                        if (target.getHitpoints() <= self.getDamage()) {
                            listOfSowEnemys.remove(gameUnit);
                            break;
                        }
                    }
                }
            }
            move.setAction(ActionType.SHOOT);
            move.setX(target.getX());
            move.setY(target.getY());
        }
    }


    void makeATarget(Trooper self) {

        //проверяем есть ли крашнутые стратегии
        Player[] playersTest = world.getPlayers();
        if (crushedPlayer == null) {
            for (Player player : playersTest) {
                if (!player.getName().equalsIgnoreCase("darkstone") && player.isStrategyCrashed()) {
                    for (ListOfPlayers listOfPlayers1 : listOfPlayers) {
                        if (listOfPlayers1.player.getId() == player.getId() && !listOfPlayers1.isDead) {
                            if (indexOfCommander != -1) {
                                needHelpFromAir = true;
                                break;
                            }
                        }
                    }
                }
            }
        }

        if (getHelpFromAir && targetTrooper == null && localTargetX == 100 && self.getType() == TrooperType.COMMANDER) {

            //реагирование на crash стратегии для игры 4 х Х
            isEnemyStrategyCrashed = false;

            Player[] playersTestCrushed = world.getPlayers();

            //перебираем весь список и если у игрока стратегия упала, запоминаем координаты разведки его юнитов (вдруг командир умрёт)...
            for (Player player : playersTestCrushed) {
                if (!player.getName().equalsIgnoreCase("darkstone") && player.isStrategyCrashed() && player.getApproximateX() != -1) {
                    for (ListOfPlayers listOfPlayers1 : listOfPlayers) {
                        if (listOfPlayers1.player.getId() == player.getId() && !listOfPlayers1.isDead) {
                            listOfPlayers1.player = player;
                            listOfPlayers1.approximateX = player.getApproximateX();
                            listOfPlayers1.approximateY = player.getApproximateY();
                            if (listOfPlayers1.player.getId() == crushedPlayer.player.getId()) {
                                crushedPlayer = listOfPlayers1;
                            }
                            break;
                        }
                    }       // ... а если стратегия игрока упала и его юниты уничтожены, то помечаем его мёртвым.
                } else if (!player.getName().equalsIgnoreCase("darkstone") && player.isStrategyCrashed() && player.getApproximateX() == -1) {
                    for (ListOfPlayers listOfPlayers1 : listOfPlayers) {
                        if (listOfPlayers1.player.getId() == player.getId() && !listOfPlayers1.isDead) {
                            listOfPlayers1.isDead = true;
                            listOfPlayers1.approximateX = -1;
                            listOfPlayers1.approximateY = -1;
                            if (listOfPlayers1.player.getId() == crushedPlayer.player.getId() && !listOfPlayers1.isDead) {
                                crushedPlayer = listOfPlayers1;
                            }
                            break;
                        }
                    }
                }
            }

            if (crushedPlayer != null && crushedPlayer.isDead) {
                crushedPlayer = null;
            }

            //если ещё не выбран крашнутый игрок, то выбираем такого
            if (crushedPlayer == null) {
                for (ListOfPlayers crushedPlayer1 : listOfPlayers) {
                    if (!crushedPlayer1.isDead && crushedPlayer1.player.isStrategyCrashed()) {
                        for (Player player : playersTestCrushed) {
                            if (crushedPlayer1.player.getId() == player.getId()) {
                                crushedPlayer = crushedPlayer1;
                            }
                        }
                    }
                }

            }

            //если крашнутый игрок ещё целевой и живой, то повторно вводим его координаты в globalTarget
            if(crushedPlayer != null) {
                for (Player player : playersTestCrushed) {
                    if (crushedPlayer.player.getId() == player.getId() && !crushedPlayer.isDead) {
                        thePoint point = testCellOnFree(player.getApproximateX(), player.getApproximateY());
                        globalTargetX = point.getX();
                        globalTargetY = point.getY();
                        isEnemyStrategyCrashed = true;
                        needHelpFromAir = false;
                        break;
                    }
                }
            }

            if (!isEnemyStrategyCrashed) {
                Player[] plr = world.getPlayers();
                boolean testOnApproximates = false;
                for (int i = 0; i < plr.length; i++) {
                    if (plr[i].getApproximateX() != -1) {
                        testOnApproximates = true;
                    }
                }
                if (testOnApproximates) {
                    double distance = 500;
                    int numIndex = -1;
                    for (int i = 0; i < plr.length; i++) {
                        Player player = plr[i];
                        if (!player.getName().equalsIgnoreCase("darkstone") && player.getApproximateX() != -1) {
                            if (self.getDistanceTo(player.getApproximateX(), player.getApproximateY()) < distance) {
                                distance = self.getDistanceTo(player.getApproximateX(), player.getApproximateY());
                                numIndex = i;
                            }
                        }
                    }

                    thePoint point = testCellOnFree(plr[numIndex].getApproximateX(), plr[numIndex].getApproximateY());
                    //thePoint point = testCellOnFree(world.getWidth() / 2, world.getHeight() / 2);
                    globalTargetX = point.getX();
                    globalTargetY = point.getY();
                    getHelpFromAir = false;
                    needHelpFromAir = false;
                }
            }
        }

        if (globalTargetX == -1 && globalTargetY == -1) {

            lastMoveX = self.getX();
            lastMoveY = self.getY();

            numOfTroopers = troopers.length;
            hpOfTroopers = new int[numOfTroopers][2];

            hpOfTroopers[0][0] = (int) troopers[indexOfMedic].getId();
            hpOfTroopers[0][1] = troopers[indexOfMedic].getHitpoints();

            hpOfTroopers[1][0] = (int) troopers[indexOfCommander].getId();
            hpOfTroopers[1][1] = troopers[indexOfCommander].getHitpoints();

            hpOfTroopers[2][0] = (int) troopers[indexOfSoldier].getId();
            hpOfTroopers[2][1] = troopers[indexOfSoldier].getHitpoints();

            if (numOfTroopers == 4) {
                hpOfTroopers[3][0] = (int) troopers[indexOfSniper].getId();
                hpOfTroopers[3][1] = troopers[indexOfSniper].getHitpoints();
            }
            if (numOfTroopers == 5) {
                hpOfTroopers[3][0] = (int) troopers[indexOfSniper].getId();
                hpOfTroopers[4][0] = (int) troopers[indexOfScout].getId();
                hpOfTroopers[3][1] = troopers[indexOfSniper].getHitpoints();
                hpOfTroopers[4][1] = troopers[indexOfScout].getHitpoints();
            }

            for (int i = 0; i < numOfTroopers; i++) {
                for (Trooper trooper : troopers) {
                    if (trooper.getId() == hpOfTroopers[i][0]) {
                        stayOnTailList.add(new thePoint(trooper.getX(), trooper.getY()));
                        break;
                    }
                }
            }

            int xt = world.getWidth() / 2, yt = world.getHeight() / 2;

            thePoint pointTemp = testCellOnFree(xt, yt);
            if (pointTemp != null) {
                globalTargetX = pointTemp.getX();
                globalTargetY = pointTemp.getY();
            }

            for (int i = 0; i < 4; i++) {
                remainingQuarters.add(i);
            }

            Player[] players = world.getPlayers();
            for (Player player : players) {
                listOfPlayers.add(new ListOfPlayers(player));
            }

//            randomTarget(self.getX(), self.getY());
        }

        if (self.getDistanceTo(globalTargetX, globalTargetY) <= MIN_DISTANCE_FOR_LOCALTARGET) {

            if (indexOfCommander != -1) {

                if (crushedPlayer != null) {
                    crushedPlayer = null;
                }
                needHelpFromAir = true;

            } else {

                boolean haveATarget = false;

                if (crushedPlayer != null) {

                    crushedPlayer.approximateX = -1;
                    crushedPlayer.approximateY = -1;

                    for (ListOfPlayers listOfPlayers1 : listOfPlayers) {
                        if (listOfPlayers1.approximateX != -1) {
                            crushedPlayer = listOfPlayers1;
                            haveATarget = true;
                            globalTargetX = listOfPlayers1.approximateX;
                            globalTargetY = listOfPlayers1.approximateY;
                            break;
                        }
                    }
                    if (!haveATarget) {
                        crushedPlayer = null;
                    }
                }

                if (!haveATarget) {
                    randomTarget(self.getX(), self.getY());
                }
            }
        }

//      Обнуление локальной цели при её достижимости

        if (self.getDistanceTo(localTargetX, localTargetY) <= MIN_DISTANCE_FOR_LOCALTARGET) {
            localTargetX = 100;
            localTargetY = 100;
            beginBattle = false;
            detectEnemyByTeam = false;
            for (Trooper trooper : troopers) {
                if (trooper.isTeammate()) {
                    LinkedList<thePoint> path = lee(self, self.getX(), self.getY(), trooper.getX(), trooper.getY(), false);
                    if (path != null && path.size() > 1 && path.size() > 8) {
                        localTargetX = trooper.getX();
                        localTargetY = trooper.getY();
                        break;
                    }
                }
            }
        }

        if (targetTrooper != null) {
            localTargetX = targetTrooper.getX();
            localTargetY = targetTrooper.getY();
        }

        if (listOfEnemyTroopers.size() != 0) {
            Trooper choosenOne = chooseEnemyOnDistance(self, listOfEnemyTroopers);
            if (choosenOne != null) {
                targetTrooper = choosenOne;
            }
        } else {
            targetTrooper = null;
        }
    }


    int getCostMoveWithStance(Trooper self) {

        if (self.getStance() == TrooperStance.STANDING) {
            return 2;
        } else if (self.getStance() == TrooperStance.KNEELING) {
            return 4;
        } else if (self.getStance() == TrooperStance.PRONE) {
            return 6;
        }

        return 6;
    }

    boolean goDown(Trooper self) {
        if (self.getActionPoints() >= game.getStanceChangeCost() && indexOfCommander != -1 && self.getDistanceTo(troopers[indexOfCommander]) <= 3) {
            if (self.getStance() == TrooperStance.STANDING || self.getStance() == TrooperStance.KNEELING) {
                move.setAction(ActionType.LOWER_STANCE);
                return true;
            }
        } else {
            if (self.getActionPoints() >= game.getStanceChangeCost() && indexOfSoldier != -1 && indexOfCommander == -1 && self.getDistanceTo(troopers[indexOfSoldier]) <= 3) {
                move.setAction(ActionType.LOWER_STANCE);
                return true;
            }
        }
        return false;
    }

    //если может убить, то идёт и добивает полуживого юнита
    boolean goAndKillTarget (Trooper self) {

        if (listOfEnemyTroopers.size() != 0) {
            for(Trooper target : listOfEnemyTroopers) {
                if (!(self.getDistanceTo(target) <= self.getShootingRange() && world.isVisible(self.getShootingRange(), self.getX(), self.getY(), self.getStance(), target.getX(), target.getY(), target.getStance()))) {

                    LinkedList<thePoint> tempList = lee(self, self.getX(), self.getY(), target.getX(), target.getY(), true);

                    if(tempList != null && tempList.size() > 1) {
                        int goUpStance = 0;

                        if (self.getStance() == TrooperStance.PRONE) {
                            goUpStance = 4;
                        } else if (self.getStance() == TrooperStance.KNEELING) {
                            goUpStance = 2;
                        } else if (self.getStance() == TrooperStance.STANDING) {
                            goUpStance = 0;
                        }

                        double leftScoreOfMove = self.getActionPoints() - goUpStance - (tempList.size() - 1 < self.getShootingRange() ? 0 : (tempList.size() - 1 - self.getShootingRange())) * game.getStandingMoveCost();

                        int moves = 0;
                        for (int i = 0; i < tempList.size(); ++i) {
                            if (getDistancePointToPoint(tempList.get(i).getX(), tempList.get(i).getY(), target.getX(), target.getY()) <= self.getShootingRange() && world.isVisible(self.getShootingRange(), tempList.get(i).getX(), tempList.get(i).getY(), TrooperStance.STANDING, target.getX(), target.getY(), target.getStance())) {
                                moves = i;
                                if (target.getHitpoints() < ((int)(leftScoreOfMove - moves * 2) / self.getShootCost()) * self.getStandingDamage()) {
                                    move.setAction(ActionType.MOVE);
                                    move.setX(tempList.get(1).getX());
                                    move.setY(tempList.get(1).getY());
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }


    boolean testForFreePassage(Trooper self) {
        if (targetTrooper != null && self.getDistanceTo(lastMoveX, lastMoveY) == 1 && isBetweenWalls(lastMoveX, lastMoveY)) {
            if (goOnPath(self, targetTrooper.getX(), targetTrooper.getY(), true)) {
                return true;
            }
        }
        return false;
    }

    boolean isBetweenWalls(int moveX, int moveY) {

        CellType[][] cells = world.getCells();

        boolean rLine = false, lLine = false, uLine = false, dLine = false;

        if (moveX + 1 >= world.getWidth()) {
            rLine = true;
        }
        if (moveX - 1 < 0) {
            lLine = true;
        }
        if (moveY + 1 >= world.getHeight()) {
            uLine = true;
        }
        if (moveY - 1 < 0) {
            dLine = true;
        }

        if ((lLine ? true : cells[moveX - 1][moveY] != CellType.FREE) && (rLine ? true : cells[moveX + 1][moveY] != CellType.FREE) || (dLine ? true : cells[moveX][moveY - 1] != CellType.FREE) && (uLine ? true : cells[moveX][moveY + 1] != CellType.FREE)) {
            return true;
        }

        return false;
    }

    thePoint testCellOnFree(int x, int y) {
        CellType[][] cells = world.getCells();
        int x1 = x;
        int y1 = y;
        int x2 = x;
        int y2 = y;
        boolean flag = true;
        do {
            for (int i = x1; i <= x2; i++) {
                for (int j = y1; j <= y2; j++) {
                    if (i < world.getWidth() && i >= 0 && j < world.getHeight() && j >= 0) {
                        if (cells[i][j] == CellType.FREE) {
                            return new thePoint(i, j);
                        }
                    }
                }
                if (flag == false) {
                    break;
                }
            }
            x1--;
            x2++;
            y1--;
            y2++;
        } while (flag);
        return null;
    }

    thePoint findStrongCell(Trooper self, int x, int y, int valueOfCell) {
        int x1 = x - 1;
        int y1 = y - 1;
        int x2 = x + 1;
        int y2 = y + 1;
        double distTeam = 50;
        Trooper teamTrooper = null;
        if(forwardTrooper != -1 && troopers[forwardTrooper].getType() != self.getType() && self.getDistanceTo(troopers[forwardTrooper]) <= 5) {
            teamTrooper = troopers[forwardTrooper];
        } else {
            for (Trooper trooper : troopers) {
                if (trooper.isTeammate() && self.getDistanceTo(trooper) <= 5) {
                    teamTrooper = trooper;
                    break;
                }
            }
        }


        for (int i = x1; i <= x2; i++) {
            for (int j = y1; j <= y2; j++) {
                if (i < world.getWidth() && i >= 0 && j < world.getHeight() && j >= 0 && getDistancePointToPoint(x, y, i, j) == 1 && !(self.getX() == i && self.getY() == j) && self.getDistanceTo(teamTrooper) < distTeam) {
                    if (trueMapOfPoints[i][j] > valueOfCell) {
                        return new thePoint(i, j);
                    }
                }
            }
        }

        return null;
    }

    thePoint findAnyCell(Trooper self) {
        int x1 = self.getX() - 1;
        int y1 = self.getY() - 1;
        int x2 = self.getX() + 1;
        int y2 = self.getY() + 1;
        double distTeam = 50;
        LinkedList<thePoint> listOfFreePoints = new LinkedList<thePoint>();

        for (int i = x1; i <= x2; i++) {
            for (int j = y1; j <= y2; j++) {
                if (i < world.getWidth() && i >= 0 && j < world.getHeight() && j >= 0 && getDistancePointToPoint(self.getX(), self.getY(), i, j) == 1 && !(self.getX() == i && self.getY() == j) && cellsInt[i][j] == -2) {

                    boolean test = false;
                    for (Trooper trooper : troopers) {
                        if (trooper.isTeammate() && trooper.getX() == i && trooper.getY() == j) {
                            test = true;
                            break;
                        }
                    }

                    if(test) {
                        continue;
                    }

                    listOfFreePoints.add(new thePoint(i, j));
                }
            }
        }

        if (listOfFreePoints.size() != 0) {
            int pnt = (int) (Math.random() * listOfFreePoints.size());
            return listOfFreePoints.get(pnt);
        }

        return null;
    }

    boolean killAnyEnemyUnit(Trooper self) {
        //пытается убить любую вражескую цель, если она убиваема

        int actionPoint = self.getActionPoints();

        //пробуем убить без использования FIELD_RATION
        for (Trooper trooper : listOfEnemyTroopers) {
            if ((trooper.getHitpoints() % self.getDamage(self.getStance()) == 0 ? trooper.getHitpoints() / self.getDamage(self.getStance()) : trooper.getHitpoints() / self.getDamage(self.getStance()) + 1) <= actionPoint / self.getShootCost()) {
                if (canShootOnTarget(self, trooper)) {
                    shootOnTarget(self, trooper);
                    return true;
                }
            }
        }

        for (Trooper trooper : listOfEnemyTroopers) {
            if ((trooper.getHitpoints() % self.getDamage(self.getStance()) == 0 ? trooper.getHitpoints() / self.getDamage(self.getStance()) : trooper.getHitpoints() / self.getDamage(self.getStance()) + 1) <= actionPoint / self.getShootCost()/* - 2*/) {
                if (testMoveUpAttack(self, trooper)) {
                    return true;
                }
                if (canShootOnTarget(self, trooper)) {
                    shootOnTarget(self, trooper);
                    return true;
                }
            }
        }

        //пробуем убить с использованием FIELD_RATION
        if (self.isHoldingFieldRation()) {
            actionPoint += 3;
        }

        for (Trooper trooper : listOfEnemyTroopers) {
            if ((trooper.getHitpoints() % self.getDamage(self.getStance()) == 0 ? trooper.getHitpoints() / self.getDamage(self.getStance()) : trooper.getHitpoints() / self.getDamage(self.getStance()) + 1) <= actionPoint / self.getShootCost()) {
                if (canShootOnTarget(self, trooper)) {
                    if (self.isHoldingFieldRation() && self.getActionPoints() >= 2 && self.getActionPoints() <= 6) {
                        move.setAction(ActionType.EAT_FIELD_RATION);
                        return true;
                    }
                    shootOnTarget(self, trooper);
                    return true;
                }
            }
        }

        for (Trooper trooper : listOfEnemyTroopers) {
            if ((trooper.getHitpoints() % self.getDamage(self.getStance()) == 0 ? trooper.getHitpoints() / self.getDamage(self.getStance()) : trooper.getHitpoints() / self.getDamage(self.getStance()) + 1) <= actionPoint / self.getShootCost()/* - 2*/) {
                if (testMoveUpAttack(self, trooper)) {
                    return true;
                }
                if (canShootOnTarget(self, trooper)) {
                    if (self.isHoldingFieldRation() && self.getActionPoints() >= 2 && self.getActionPoints() <= 6) {
                        move.setAction(ActionType.EAT_FIELD_RATION);
                        return true;
                    }
                    shootOnTarget(self, trooper);
                    return true;
                }
            }
        }

        return false;
    }

    boolean hpIsChanged(Trooper self) {
        for (int i = 0; i < troopers.length; i++) {
            for (int j = 0; j < hpOfTroopers.length; j++) {
                if (troopers[i].getId() == hpOfTroopers[j][0] && troopers[i].getHitpoints() < hpOfTroopers[j][1]) {

                    boolean isNotVisibleByEnemy = true;

                    for (Trooper trooper : listOfEnemys) {
                        if(troopers[i].getDistanceTo(trooper) <= trooper.getShootingRange() + 1) {
                            isNotVisibleByEnemy = false;
                            istroopersUnderAttack = false;
                            trooperUnderAttack = -1;
                        }
                    }

                    if(isNotVisibleByEnemy) {
                        trooperUnderAttack = (int) troopers[i].getId();
                        localTargetX = globalTargetX;
                        localTargetY = globalTargetY;

                        lastPositionPointForTrooperIsUnderAttack = new thePoint(troopers[i].getX(), troopers[i].getY());

                        if (targetTrooper == null) {
                            enemyInAmbush = true;
                        } else {
                            detectEnemyByTeam = true;
                        }
                        return true;
                    }

                    return false;
                }
            }
        }
        return false;
    }


    void processingHpOfTroopers() {
        for (int i = 0; i < troopers.length; i++) {
            if(troopers[i].isTeammate()) {
                for (int j = 0; j < hpOfTroopers.length; j++) {
                    if (troopers[i].getId() == hpOfTroopers[j][0]) {
                        hpOfTroopers[j][1] = troopers[i].getHitpoints();
                        break;
                    }
                }
            }
        }
    }


    void underAttack(Trooper self) {
        if (istroopersUnderAttack && trooperUnderAttack == self.getId() && listOfEnemyTroopers.size() == 0) {
            thePoint point = goToTheAllegedEnemy(self);
            if (point != null) {
                point = testCellOnFree(point.getX(), point.getY());
                localTargetX = point.getX();
                localTargetY = point.getY();
            }
        } else if (istroopersUnderAttack && trooperUnderAttack != self.getId() && trooperUnderAttack != -1 && listOfEnemyTroopers.size() == 0) {
            if (self.getDistanceTo(localTargetX, localTargetY) <= 3) {
                Trooper troop = null;
                for (Trooper trooper : troopers) {
                    if (trooper.getId() == trooperUnderAttack) {
                        troop = trooper;
                        break;
                    }
                }
                thePoint point = goToTheAllegedEnemy(troop);
                point = testCellOnFree(point.getX(), point.getY());
                if (point != null) {
                    localTargetX = point.getX();
                    localTargetY = point.getY();
                }
            }
        }
    }

    boolean goToMedic(Trooper self) {

        if (killAnyEnemyUnit(self)) {
            return true;
        }

        if (goAndKillTarget(self)) {
            return true;
        }

        if (self.getHitpoints() < HP_WHEN_GO_MEDIC && self.getActionPoints() >= getCostMoveWithStance(self) && self.getType() != TrooperType.FIELD_MEDIC) {

            if (indexOfMedic != -1 && self.getDistanceTo(troopers[indexOfMedic]) > 1 && self.getType() != TrooperType.SNIPER) {

                LinkedList<thePoint> path1 = lee(self, self.getX(), self.getY(), troopers[indexOfMedic].getX(), troopers[indexOfMedic].getY(), true);
                LinkedList<thePoint> path2 = lee(self, self.getX(), self.getY(), troopers[indexOfMedic].getX(), troopers[indexOfMedic].getY(), false);

                if (path1 != null && path1.size() <= path2.size() + 5) {

                    if (fireAndExit(self, path1)) {
                        return true;
                    }

                    if(self.getActionPoints() < 6) {
                        if(safeStance == null) {
                            if (clearSelfArea(self, 4, 2)) {
                                //ничего не делаем
                            } else if (clearSelfArea(self, 4, 3)) {
                                if (self.getStance() != TrooperStance.KNEELING) {
                                    if (self.getStance() == TrooperStance.STANDING) {
                                        move.setAction(ActionType.LOWER_STANCE);
                                        return true;
                                    }
                                    if (self.getStance() == TrooperStance.PRONE) {
                                        move.setAction(ActionType.RAISE_STANCE);
                                        return true;
                                    }
                                }
                            } else {
                                if (self.getStance() != TrooperStance.PRONE) {
                                    move.setAction(ActionType.LOWER_STANCE);
                                    return true;
                                }
                            }
                        } else {
                            if (safeStance != self.getStance() || self.getStance() != TrooperStance.PRONE) {
                                move.setAction(ActionType.LOWER_STANCE);
                                return true;
                            }
                        }

                        move.setAction(ActionType.END_TURN);
                        return true;
                    }

                    if (goOnPath(self, troopers[indexOfMedic].getX(), troopers[indexOfMedic].getY(), true)) {
                        return true;
                    }
                } else if (path1 != null && path1.size() > path2.size() + 5) {

                    if (fireAndExit(self, path1)) {
                        return true;
                    }

                    if(self.getActionPoints() < 6) {

                        if(safeStance == null) {
                            if (clearSelfArea(self, 4, 2)) {
                                //ничего не делаем
                            } else if (clearSelfArea(self, 4, 3)) {
                                if (self.getStance() != TrooperStance.KNEELING) {
                                    if (self.getStance() == TrooperStance.STANDING) {
                                        move.setAction(ActionType.LOWER_STANCE);
                                        return true;
                                    }
                                    if (self.getStance() == TrooperStance.PRONE) {
                                        move.setAction(ActionType.RAISE_STANCE);
                                        return true;
                                    }
                                }
                            } else {
                                if (self.getStance() != TrooperStance.PRONE) {
                                    move.setAction(ActionType.LOWER_STANCE);
                                    return true;
                                }
                            }
                        } else {
                            if (safeStance != self.getStance() || self.getStance() != TrooperStance.PRONE) {
                                move.setAction(ActionType.LOWER_STANCE);
                                return true;
                            }
                        }

                        move.setAction(ActionType.END_TURN);
                        return true;
                    }

                    if (goOnPath(self, troopers[indexOfMedic].getX(), troopers[indexOfMedic].getY(), false)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    boolean canSeeOrCanShoot(Trooper trooper1, Trooper trooper2, /*canSee = true, canShoot = false*/boolean canSeeOrCanShoot) {
        if (canSeeOrCanShoot) {
            return world.isVisible(trooper1.getVisionRange(), trooper1.getX(), trooper1.getY(), trooper1.getStance(), trooper2.getX(), trooper2.getY(), trooper2.getStance());
        } else {
            return world.isVisible(trooper1.getShootingRange(), trooper1.getX(), trooper1.getY(), trooper1.getStance(), trooper2.getX(), trooper2.getY(), trooper2.getStance());
        }
    }

    boolean saveOursSouls(Trooper self) {
        boolean isJitters = false;
        double minDistance = 50;
        for (Trooper troop : troopers) {
            if (targetTrooper != null && troop.isTeammate() && troop.getDistanceTo(targetTrooper) <= minDistance && self.getType() != troop.getType()) {
                minDistance = troop.getDistanceTo(targetTrooper);
            }
        }
        if (targetTrooper != null && self.getDistanceTo(targetTrooper) <= minDistance + 2 && minDistance != 50) {
            isJitters = true;
        }

        if (self.getHitpoints() < 40 && isJitters) {
            thePoint point = findNotAchievableTail(self, true, 0);

            if (point != null && self.getX() == point.getX() && self.getY() == point.getY()) {
                if(self.getActionPoints() >= game.getStanceChangeCost()) {
                    move.setAction(ActionType.LOWER_STANCE);
                    return true;
                }
            }

            if (point != null) {

                LinkedList<thePoint> pathTempT = lee(self, self.getX(), self.getY(), point.getX(), point.getY(), true);
                LinkedList<thePoint> pathTempF = lee(self, self.getX(), self.getY(), point.getX(), point.getY(), false);

                int actionPoints = self.getActionPoints();
                int goUpStance = 0;
                int afterUseRationFieldPoints = 3;

                if (self.getStance() == TrooperStance.PRONE) {
                    goUpStance = 4;
                } else if (self.getStance() == TrooperStance.KNEELING) {
                    goUpStance = 2;
                } else if (self.getStance() == TrooperStance.STANDING) {
                    goUpStance = 0;
                }

                if (self.isHoldingFieldRation()) {
                    actionPoints += afterUseRationFieldPoints;
                }

                actionPoints -= goUpStance;

                if (pathTempT != null && pathTempT.size() > 1 && pathTempT.size() <= pathTempF.size() + 5) {
                    if (actionPoints >= (pathTempT.size() - 1) * game.getStandingMoveCost()) {
                        if (actionPoints - (pathTempT.size() - 1) * game.getStandingMoveCost() < 3) {
                            useFieldRation(self);
                            return true;
                        }
                        if (self.getStance() != TrooperStance.STANDING && self.getActionPoints() >= game.getStanceChangeCost()) {
                            move.setAction(ActionType.RAISE_STANCE);
                            return true;
                        }
                        if (goOnPath(self, point.getX(), point.getY(), true)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    //вычисляем расстояние между ближайшим юнитом и юнитом под атакой, и откладываем его в другую сторону от ближайшего юнита
    thePoint goToTheAllegedEnemy(Trooper self) {
        int x;
        int y;
        thePoint point = null;

        LinkedList<Trooper> listTroopers = new LinkedList<Trooper>();
        if(trooperUnderAttack != -1) {
            for (Trooper trooper : troopers) {
                if (trooper.getId() != trooperUnderAttack && trooper.isTeammate()) {
                    listTroopers.add(trooper);
                }
            }
        }

        if (listTroopers.size() != 0) {
            double dist = 7;

            ArrayList<Trooper> closeTeamTroopers = new ArrayList<Trooper>();
            ArrayList<thePoint> approximatePoints = new ArrayList<thePoint>();

            for (Trooper trooper : listTroopers) {
                if (self.getDistanceTo(trooper) < dist) {
                    closeTeamTroopers.add(trooper);
                }
            }

            for (Trooper testTrooper : closeTeamTroopers) {

                if (self.getX() < testTrooper.getX()) {
                    x = self.getX() - 6;
                } else if (self.getX() > testTrooper.getX()) {
                    x = self.getX() + 6;
                } else {
                    x = self.getX();
                }

                if (self.getY() < testTrooper.getY()) {
                    y = self.getY() - 6;
                } else if (self.getY() > testTrooper.getY()) {
                    y = self.getY() + 6;
                } else {
                    y = self.getY();
                }

                if (Math.abs(self.getX() - testTrooper.getX()) >= 3 && Math.abs(self.getY() - testTrooper.getY()) < 3) {
                    if(Math.abs(self.getX() - testTrooper.getX()) >= 3 && Math.abs(self.getY() - testTrooper.getY()) < 2) {
                        y /= 3;
                    } else {
                        y /= 2;
                    }
                } if (Math.abs(self.getX() - testTrooper.getX()) < 3 && Math.abs(self.getY() - testTrooper.getY()) >= 3) {
                    if(Math.abs(self.getX() - testTrooper.getX()) < 2 && Math.abs(self.getY() - testTrooper.getY()) >= 3) {
                        x /= 3;
                    } else {
                        x /= 2;
                    }
                }

                approximatePoints.add(new thePoint(x, y));
            }

            if (approximatePoints.size() != 0) {

                int x1 = 0;
                int y1 = 0;

                for (thePoint point1 : approximatePoints) {
                    x1 += point1.getX();
                    y1 += point1.getY();
                }
                point = new thePoint(x1 / approximatePoints.size(), y1 / approximatePoints.size());
            }
        }

        if(point != null) {
            enemyInAmbush = true;
            return point;
        } else {
            return null;
        }
    }

    boolean clearSelfArea(Trooper self, int radius, int freeCellsOrFreeVisionStandingOrLOWMIDCover) {
        // freeCellsOrFreeVis.... - проверяет ячейки в радиусе radius на то, что они :
        // 0 - все FREE
        // 1 - не имеют рядом HIGH COVER
        // 2 - не имеют рядом LOW и MIDDLE COVER
        // 3 - не имеют рядом LOW COVER
        //
        CellType[][] cells = world.getCells();
        int x1 = self.getX() - radius;
        int y1 = self.getY() - radius;
        int x2 = self.getX() + radius;
        int y2 = self.getY() + radius;

        for (int i = 1; i <= radius; i++) {
            if (x1 < 0) {
                x1++;
            }

            if (y1 < 0) {
                y1++;
            }

            if (x2 >= world.getWidth()) {
                x2--;
            }

            if (y2 >= world.getHeight()) {
                y2--;
            }
        }

        for (int i = x1; i <= x2; i++) {
            for (int j = y1; j <= y2; j++) {
                switch (freeCellsOrFreeVisionStandingOrLOWMIDCover) {
                    case 0 : {
                        if (cells[i][j] != CellType.FREE) {
                            return false;
                        }
                        break;
                    }

                    case 1 : {
                        if (cells[i][j] == CellType.HIGH_COVER) {
                            return false;
                        }
                        break;
                    }

                    case 2 : {
                        if (!(cells[i][j] == CellType.HIGH_COVER || cells[i][j] == CellType.FREE)) {
                            return false;
                        }
                        break;
                    }

                    case 3 : {
                        if (cells[i][j] == CellType.LOW_COVER) {
                            return false;
                        }
                        break;
                    }
                }
            }
        }

        return true;
    }


    boolean fireAndExit (Trooper self, LinkedList<thePoint> path) {

        int actionPoints = self.getActionPoints();
        int goUpStance = 0;

        if (self.getStance() == TrooperStance.PRONE) {
            goUpStance = 4;
        } else if (self.getStance() == TrooperStance.KNEELING) {
            goUpStance = 2;
        } else if (self.getStance() == TrooperStance.STANDING) {
            goUpStance = 0;
        }

        if (path.size() > 2) {
            actionPoints -= goUpStance;
        }

        if ((path.size() - 1) * 2 > actionPoints) {

            boolean isPosibleShootByEnemy = false;

            for (Trooper trooper : listOfEnemyTroopers) {
                if (canSeeOrCanShoot(trooper, self, true) || canSeeOrCanShoot(trooper, self, false)) {
                    isPosibleShootByEnemy = true;
                }
            }

            if (isPosibleShootByEnemy) {

                thePoint point1 = findNotAchievableTail(self, false, 0);
                thePoint point2 = findNotAchievableTail(self, true, 0);
                thePoint point;

                if (point1 != null && self.getX() == point1.getX() && self.getY() == point1.getY()) {

                    int goDownStance = 0;
                    if (safeStance == TrooperStance.KNEELING && self.getStance() == TrooperStance.STANDING) {
                        goDownStance = 2;
                    } else if (safeStance == TrooperStance.PRONE && self.getStance() == TrooperStance.KNEELING) {
                        goDownStance = 2;
                    } else if (safeStance == TrooperStance.PRONE && self.getStance() == TrooperStance.STANDING) {
                        goDownStance = 4;
                    }

                    if (self.getActionPoints() >= goDownStance + self.getShootCost()) {

                        for (Trooper trooper : listOfEnemyTroopers) {
                            if (canShootOnTarget(self, trooper)) {
                                shootOnTarget(self, trooper);
                                return true;
                            }
                        }

                        if (self.getStance() != TrooperStance.PRONE && game.getStanceChangeCost() >= self.getActionPoints()) {
                            move.setAction(ActionType.LOWER_STANCE);
                            return true;
                        }

                    } else {
                        if(self.getActionPoints() >= game.getStanceChangeCost()) {
                            move.setAction(ActionType.LOWER_STANCE);
                            return true;
                        }
                    }

                }

                if (!(point1 == null && point2 == null)) {

                    if (point1 != null && point2 != null) {
                        if (self.getDistanceTo(point1.getX(), point2.getY()) <= self.getDistanceTo(point2.getX(), point2.getY())) {
                            point = point2;
                        } else {
                            point = point1;
                        }
                    } else if (point1 != null) {
                        point = point1;
                    } else {
                        point = point2;
                    }

                    LinkedList<thePoint> tempPath = lee(self, self.getX(), self.getY(), point.getX(), point.getY(), true);

                    if (tempPath != null && tempPath.size() > 1 && (tempPath.size() - 1) * 2 + self.getShootCost() <= actionPoints) {

                        for (Trooper trooper : listOfEnemyTroopers) {
                            if (canShootOnTarget(self, trooper)) {
                                shootOnTarget(self, trooper);
                                return true;
                            }
                        }

                        if (self.getStance() != TrooperStance.STANDING && game.getStanceChangeCost() <= actionPoints) {
                            move.setAction(ActionType.RAISE_STANCE);
                            return true;
                        }

                        if (goOnPath(self, point.getX(), point.getY(), true)) {
                            return true;
                        }

                    } else if (tempPath != null && tempPath.size() > 1 && (tempPath.size() - 1) * 2 <= actionPoints) {

                        if (self.getStance() != TrooperStance.STANDING && game.getStanceChangeCost() <= actionPoints) {
                            move.setAction(ActionType.RAISE_STANCE);
                            return true;
                        }

                        if (goOnPath(self, point.getX(), point.getY(), true)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    int[][] getMapOfPoints(Trooper self) {

        int H = world.getHeight();
        int W = world.getWidth();

        if (world.getMoveIndex() == 0 && cellsInt == null) {

            cellsInt = new int[W][H];                // рабочее поле

            if (cellsInt[0][0] == 0) {
                CellType[][] cells = world.getCells();

                for (int i = 0; i < W; i++) {
                    for (int j = 0; j < H; j++) {
                        if (cells[i][j].name() == "FREE") {
                            cellsInt[i][j] = -2;
                        } else {
                            cellsInt[i][j] = -1;
                        }
                    }
                }
            }
        }

        int[][] mapOfPoints = new int[cellsInt.length][cellsInt[0].length];
        for (int i = 0; i < W; i++) {
            mapOfPoints[i] = Arrays.copyOf(cellsInt[i], cellsInt[i].length);
        }

        for (int i = 0; i < mapOfPoints.length; i++) {
            for (int j = 0; j < mapOfPoints[0].length; j++) {
                if (mapOfPoints[i][j] == -1) {
                    mapOfPoints[i][j] = 0;
                }
            }
        }

        for (int i = 0; i < mapOfPoints.length; i++) {
            for (int j = 0; j < mapOfPoints[0].length; j++) {
                if(mapOfPoints[i][j] == -2) {

                    boolean rLine = false, lLine = false, uLine = false, dLine = false;

                    if (i + 1 >= world.getWidth()) {
                        rLine = true;
                    }
                    if (i - 1 < 0) {
                        lLine = true;
                    }
                    if (j + 1 >= world.getHeight()) {
                        uLine = true;
                    }
                    if (j - 1 < 0) {
                        dLine = true;
                    }

                    if(!dLine && !rLine && !uLine && !lLine) {

                        ArrayList<thePoint> lineLeft = new ArrayList<>();
                        ArrayList<thePoint> lineRight = new ArrayList<>();
                        ArrayList<thePoint> lineUp = new ArrayList<>();
                        ArrayList<thePoint> lineDown = new ArrayList<>();

                        //для всех кроме граничных случаев
                        for (int a = i - 1, b = j - 1; a <= i + 1; a++) {
                            if (mapOfPoints[a][b] == 0) {
                                lineDown.add(new thePoint(a, b));
                            }
                        }

                        for (int a = i - 1, b = j - 1; b <= j + 1; b++) {
                            if (mapOfPoints[a][b] == 0) {
                                lineLeft.add(new thePoint(a, b));
                            }
                        }

                        for (int a = i + 1, b = j - 1; b <= j + 1; b++) {
                            if (mapOfPoints[a][b] == 0) {
                                lineRight.add(new thePoint(a, b));
                            }
                        }

                        for (int a = i - 1, b = j + 1; a <= i + 1; a++) {
                            if (mapOfPoints[a][b] == 0) {
                                lineUp.add(new thePoint(a, b));
                            }
                        }

                        ArrayList<ArrayList<thePoint>> refMas = new ArrayList<>();
                        refMas.add(lineLeft);
                        refMas.add(lineRight);
                        refMas.add(lineDown);
                        refMas.add(lineUp);


                        for(ArrayList<thePoint> line : refMas) {
                            if (line.size() == 2 && getDistancePointToPoint(line.get(0).getX(), line.get(0).getY(), line.get(1).getX(), line.get(1).getY()) == 1) {
                                mapOfPoints[i][j] = 2;
                                break;
                            }
                            if (line.size() == 1 && line.get(0).getX() == (line == lineUp ? i : line == lineDown ? i : line == lineLeft ? i - 1 : i + 1) && line.get(0).getY() == (line == lineUp ? j + 1 : line == lineDown ? j - 1 : line == lineLeft ? j : j)) {
                                mapOfPoints[i][j] = 2;
                            }
                        }

                        for(ArrayList<thePoint> line : refMas) {
                            if (line.size() == 2 && getDistancePointToPoint(line.get(0).getX(), line.get(0).getY(), line.get(1).getX(), line.get(1).getY()) == 2 && !(mapOfPoints[i - 1][j] == mapOfPoints[i + 1][j] && mapOfPoints[i - 1][j] == 0 || mapOfPoints[i][j - 1] == mapOfPoints[i][j + 1] && mapOfPoints[i][j - 1] == 0)) {
                                mapOfPoints[i][j] = 4;
                                break;
                            }
                        }
                    }
                }
            }
        }

        for (int i = 0; i < mapOfPoints.length; i++) {
            for (int j = 0; j < mapOfPoints[0].length; j++) {
                if (mapOfPoints[i][j] == -2) {
                    mapOfPoints[i][j] = 5;
                }
            }
        }


        /*for (Trooper trooper : troopers) {
            if (self.getType() != trooper.getType() && trooper.isTeammate()) {
                mapOfPoints[trooper.getX()][trooper.getY()] = 1;
            }
        }*/

        /*System.out.println("--------------------------------mapOfPoints---------------------------");
        for (int j = 0; j < H; j++) {
            for (int i = 0; i < W; i++) {
                if(mapOfPoints[i][j] >= 0 && mapOfPoints[i][j] < 10) {
                    System.out.print(" " + mapOfPoints[i][j] + " ");
                } else {
                    System.out.print(mapOfPoints[i][j] + " ");
                }
            }
            System.out.println();
            for (int m = 0; m < 3; m++) {
                System.out.print("");
            }
        }*/

        return mapOfPoints;
    }

    //обрабатываем стрельбу и убегание на безопасную и невидимую ячейку
    boolean shootAndGoToSafePlace (Trooper self, Trooper target, boolean canSeeOrShoot) {

        thePoint point = null;

        //если враг видень или досягаем для стрельбы, то идти как обычно, если юнит не видит никого, то отходить

        boolean flag = false;
        if(istroopersUnderAttack && trooperUnderAttack == self.getId()) {
            for (Trooper trooper : listOfEnemyTroopers) {
                if (!trooper.isTeammate() && (canSeeOrCanShoot(self, trooper, true) || canSeeOrCanShoot(self, trooper, false))) {
                    flag = true;
                }
            }
        }

        if(!flag) {
            point = findNotAchievableTail(self, canSeeOrShoot, 0);
        } else {
            point = findNotAchievableTail(self, canSeeOrShoot, 3);
            if (point == null) {
                point = findNotAchievableTail(self, canSeeOrShoot, 2);
                if (point == null) {
                    point = findNotAchievableTail(self, canSeeOrShoot, 1);
                    if (point == null) {
                        point = findNotAchievableTail(self, canSeeOrShoot, 0);
                    }
                }
            }
        }

        if (point != null && self.getX() == point.getX() && self.getY() == point.getY()) {

            int goDownStance = 0;
            if (self.getStance() == TrooperStance.STANDING && safeStance == TrooperStance.KNEELING) {
                goDownStance = 2;
            } else if (self.getStance() == TrooperStance.KNEELING && safeStance == TrooperStance.PRONE) {
                goDownStance = 2;
            } else if (self.getStance() == TrooperStance.STANDING && safeStance == TrooperStance.PRONE) {
                goDownStance = 4;
            }

            if (self.getActionPoints() >= goDownStance + self.getShootCost()) {

                if (targetTrooper != null && canShootOnTarget(self, targetTrooper)) {
                    shootOnTarget(self, targetTrooper);
                    return true;
                }

                for (Trooper trooper : listOfEnemyTroopers) {
                    if (canShootOnTarget(self, trooper)) {
                        shootOnTarget(self, trooper);
                        return true;
                    }
                }


                int goSafePoint = 0;
                if (safeStance == TrooperStance.STANDING) {
                    goSafePoint = 0;
                } else if (safeStance == TrooperStance.KNEELING) {
                    goSafePoint = 2;
                } else if (safeStance == TrooperStance.PRONE) {
                    goSafePoint = 4;
                }

                LinkedList<thePoint> tempPath = lee(self, self.getX(), self.getY(), point.getX(), point.getY(), true);
                if ((tempPath == null || tempPath.size() < 2) && self.getActionPoints() >= 4 + goSafePoint || tempPath!= null && tempPath.size() > 1 && self.getActionPoints() - (tempPath.size() - 1) * 2 - 4 >= 0) {
                    goToSafePlace = false;
                    safeStance = null;
                    safePoint = null;
                    if (goOnPath(self, target.getX(), target.getY(), true)) {
                        return true;
                    }
                }

                if (self.getStance() != TrooperStance.PRONE && self.getActionPoints() >= game.getStanceChangeCost()) {
                    move.setAction(ActionType.LOWER_STANCE);
                    if (self.getStance() == TrooperStance.STANDING && safeStance == TrooperStance.KNEELING || self.getStance() == TrooperStance.KNEELING && safeStance == TrooperStance.PRONE) {
                        safePoint = null;
                        goToSafePlace = false;
                        idOfTrooperStop = (int) self.getId();
                        saveMoveSafePlace = world.getMoveIndex();
                    }
                    return true;
                }

            } else {

                int goSafePoint = 0;
                if (safeStance == TrooperStance.STANDING) {
                    goSafePoint = 0;
                } else if (safeStance == TrooperStance.KNEELING) {
                    goSafePoint = 2;
                } else if (safeStance == TrooperStance.PRONE) {
                    goSafePoint = 4;
                }

                LinkedList<thePoint> tempPath = lee(self, self.getX(), self.getY(), point.getX(), point.getY(), true);
                if ((tempPath == null || tempPath.size() < 2) && self.getActionPoints() >= 4 + goSafePoint || tempPath!= null && tempPath.size() > 1 && self.getActionPoints() - (tempPath.size() - 1) * 2 - 4 >= 0) {
                    goToSafePlace = false;
                    safeStance = null;
                    safePoint = null;
                    if (goOnPath(self, target.getX(), target.getY(), true)) {
                        return true;
                    }
                }

                if (self.getStance() != TrooperStance.PRONE && self.getActionPoints() >= game.getStanceChangeCost()) {
                    move.setAction(ActionType.LOWER_STANCE);
                    if (self.getStance() == TrooperStance.STANDING && safeStance == TrooperStance.KNEELING || self.getStance() == TrooperStance.KNEELING && safeStance == TrooperStance.PRONE) {
                        safePoint = null;
                        goToSafePlace = false;
                        idOfTrooperStop = (int) self.getId();
                        saveMoveSafePlace = world.getMoveIndex();
                    }
                    return true;
                }

                move.setAction(ActionType.END_TURN);
                return true;
            }

        }

        LinkedList<thePoint> tempPath;

        if (point != null && target != null) {

            tempPath = lee(self, self.getX(), self.getY(), point.getX(), point.getY(), true);

            if (tempPath != null && tempPath.size() > 1 && (tempPath.size() - 1) * getCostMoveWithStance(self) + self.getShootCost() <= self.getActionPoints()) {

                if (canShootOnTarget(self, target)) {
                    shootOnTarget(self, target);
                    return true;
                } else if (listOfEnemyTroopers.size() != 0 ) {
                    for (Trooper trooper : listOfEnemyTroopers) {
                        if(canShootOnTarget(self, trooper)) {
                            shootOnTarget(self, trooper);
                            return true;
                        }
                    }

                    if (self.getActionPoints() - (tempPath.size() - 1) * getCostMoveWithStance(self) >= 4) {
                        isAfterExplore = true;
                        if (goOnPath(self, target.getX(), target.getY(), true)) {
                            return true;
                        }
                    }

                    if (goOnPath(self, point.getX(), point.getY(), true)) {
                        return true;
                    }

                }

            } else if(tempPath != null && tempPath.size() > 1 && (tempPath.size() - 1) * game.getStandingMoveCost() <= self.getActionPoints()){

                LinkedList<thePoint> tempPath1 = lee(self, self.getX(), self.getY(), point.getX(), point.getY(), true);

                if (tempPath1!= null && tempPath1.size() > 1 && self.getActionPoints() - (tempPath1.size() - 1) * game.getStandingMoveCost() >= 4) {
                    if (goOnPath(self, target.getX(), target.getY(), true)) {
                        return true;
                    }
                }

                if (goOnPath(self, point.getX(), point.getY(), true)) {
                    return true;
                }
            }
        }

        return false;
    }

    thePoint findCloseCell(Trooper self, int targetX, int targetY, boolean isWithTroopers) {
        int x1 = -1;
        int y1 = -1;
        int W = world.getWidth();
        int H = world.getHeight();
        int BLANK = -2;
        double dist = 6;
        int goUpStance = 0;

        if (self.getStance() == TrooperStance.PRONE) {
            goUpStance = 4;
        } else if (self.getStance() == TrooperStance.KNEELING) {
            goUpStance = 2;
        } else if (self.getStance() == TrooperStance.STANDING) {
            goUpStance = 0;
        }

        int[][] cellsIntTemp = new int[W][];
        for (int i = 0; i < W; i++) {
            cellsIntTemp[i] = Arrays.copyOf(cellsInt[i], cellsInt[i].length);
        }
        LinkedList<thePoint> tempPath1 = lee(self, self.getX(), self.getY(), targetX, targetY, true);
        if (tempPath1 != null && tempPath1.size() > 1) {
            for (int i = tempPath1.size() - 1; i >= 0; i--) {
                if (getDistancePointToPoint(tempPath1.get(i).getX(), tempPath1.get(i).getY(), targetX, targetY) <= dist) {
                    LinkedList<thePoint> tempPath = lee(self, self.getX(), self.getY(), tempPath1.get(i).getX(), tempPath1.get(i).getY(), isWithTroopers);
                    if (tempPath != null && tempPath.size() > 1 && self.getActionPoints() >= (tempPath.size() - 1) * game.getStandingMoveCost() + goUpStance) {
                        for (Trooper trooper : troopers) {
                            if (trooper.getX() == tempPath1.get(i).getX() && trooper.getY() == tempPath1.get(i).getY()) {
                                tempPath = null;
                                break;
                            }
                        }

                        if (tempPath != null && tempPath.size() > 1) {
                            x1 = tempPath1.get(i).getX();
                            y1 = tempPath1.get(i).getY();
                            isAfterExplore = true;
                            break;
                        }
                    }
                }
            }
        }

        if (x1 == -1 && tempPath1 != null && tempPath1.size() > 1 && self.getActionPoints() >= 4) {
            for (Trooper trooper : troopers) {
                if (trooper.getX() == tempPath1.get(1).getX() && trooper.getY() == tempPath1.get(1).getY()) {
                    tempPath1 = null;
                    break;
                }
            }
            if (tempPath1 != null && tempPath1.size() > 1) {
                x1 = tempPath1.get(1).getX();
                y1 = tempPath1.get(1).getY();
                isAfterExplore = true;
            }
        }

        if (x1 != -1 && y1 != -1) {
            return new thePoint(x1, y1);
        } else {
            return null;
        }
    }

    boolean throwGrenadeInMirage(Trooper self) {
        if (listOfSowEnemys.size() >= 2 && !isThrowGrenadeOnSowTroopers) {
            if(self.getActionPoints() >= ACTION_POINT_OF_GRENADE_THROW) {
                for (GameUnit gameUnit : listOfSowEnemys) {
                    if (self.getDistanceTo(gameUnit.trooper.getX(), gameUnit.trooper.getY()) <= 5) {
                        useGrenade(self, gameUnit.trooper.getX(), gameUnit.trooper.getY());
                        isThrowGrenadeOnSowTroopers = true;
                        for (Player player : world.getPlayers()) {
                            if (player.getName().equalsIgnoreCase("darkstone")) {
                                myScore = player.getScore();
                                break;
                            }
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }


    boolean testMoveUpAttack(Trooper self, Trooper target) {

        int goUpStance = 0;
        if (world.isVisible(self.getShootingRange(), self.getX(), self.getY(), self.getStance(), target.getX(), target.getY(), target.getStance())) {
            goUpStance = 0;
        } else if (self.getStance() == TrooperStance.KNEELING && world.isVisible(self.getShootingRange(), self.getX(), self.getY(), TrooperStance.STANDING, target.getX(), target.getY(), target.getStance())) {
            goUpStance = 2;
        } else if (self.getStance() == TrooperStance.PRONE && world.isVisible(self.getShootingRange(), self.getX(), self.getY(), TrooperStance.KNEELING, target.getX(), target.getY(), target.getStance())) {
            goUpStance = 2;
        } else if (self.getStance() == TrooperStance.PRONE && world.isVisible(self.getShootingRange(), self.getX(), self.getY(), TrooperStance.STANDING, target.getX(), target.getY(), target.getStance())) {
            goUpStance = 4;
        }

        if (goUpStance == 2) {
            if(self.getActionPoints() >= game.getStanceChangeCost()) {
                move.setAction(ActionType.RAISE_STANCE);
                return true;
            }
        } else if (goUpStance == 4) {
            if(self.getActionPoints() >= game.getStanceChangeCost()) {
                move.setAction(ActionType.RAISE_STANCE);
                return true;
            }
        }

        return false;
    }

    boolean testMoveDownAttack(Trooper self, Trooper target) {

        int goDownStance = 0;
        if (world.isVisible(self.getShootingRange(), self.getX(), self.getY(), self.getStance(), target.getX(), target.getY(), target.getStance())) {
            goDownStance = 0;
        } else if (self.getStance() == TrooperStance.KNEELING && world.isVisible(self.getShootingRange(), self.getX(), self.getY(), TrooperStance.PRONE, target.getX(), target.getY(), target.getStance())) {
            goDownStance = 2;
        } else if (self.getStance() == TrooperStance.STANDING && world.isVisible(self.getShootingRange(), self.getX(), self.getY(), TrooperStance.KNEELING, target.getX(), target.getY(), target.getStance())) {
            goDownStance = 2;
        } else if (self.getStance() == TrooperStance.STANDING && world.isVisible(self.getShootingRange(), self.getX(), self.getY(), TrooperStance.PRONE, target.getX(), target.getY(), target.getStance())) {
            goDownStance = 4;
        }

        if (goDownStance == 2) {
            if(self.getActionPoints() >= game.getStanceChangeCost()) {
                move.setAction(ActionType.LOWER_STANCE);
                return true;
            }
        } else if (goDownStance == 4) {
            if(self.getActionPoints() >= game.getStanceChangeCost()) {
                move.setAction(ActionType.LOWER_STANCE);
                return true;
            }
        }

        return false;
    }

    private void orderMove(Trooper self) {
        if(!isOrder) {

            if (listOfOrderMovesOfTroopers.size() != hpOfTroopers.length) {
                boolean isContains = true;
                for (TrooperType trooperType : listOfOrderMovesOfTroopers) {
                    if (self.getType() == trooperType) {
                        isContains = false;
                    }
                }
                if (isContains) {
                    listOfOrderMovesOfTroopers.add(self.getType());
                }
            }

            if (listOfOrderMovesOfTroopers.size() == hpOfTroopers.length) {         //Здесь именно Troopers! Проверка на заполненность предыдущего списка.

                Player[] players = world.getPlayers();

                for (Player player : players) {
                    if (player.getName().equals("darkstone")) {
                        if(listOfOrderMovesOfPlayers.size() == 0) {
                            listOfOrderMovesOfPlayers.add(player);
                        }
                    } else if (listOfOrderMovesOfPlayersTemp != null) {
                        for (Player player1 : listOfOrderMovesOfPlayersTemp) {
                            if (player.getId() == player1.getId() && player.getScore() != player1.getScore()) {

                                int scoreDifference = player.getScore() - player1.getScore();

                                //пытаемся определить порядох ходов игроков по снайперу
                                if (self.getType() == TrooperType.SNIPER && scoreDifference >= 0) {
                                    for (TrooperStance trooperStance : TrooperStance.values()) {
                                        int damage = getDamageTrooperInStance(self.getType(), trooperStance);
                                        if (scoreDifference < 145 && scoreDifference != 60 && scoreDifference != 80 && scoreDifference != 85 && scoreDifference != 105 && scoreDifference != 120 && scoreDifference != 140 && (scoreDifference % damage == 0 || (scoreDifference < damage) ? false : (scoreDifference - 25) % damage == 0 || (scoreDifference < damage) ? false : (scoreDifference - 50) % damage == 0)) {
                                            boolean isConsist = false;
                                            for (Player player2 : listOfOrderMovesOfPlayers) {
                                                if (player2.getId() == player.getId()) {
                                                    isConsist = true;
                                                }
                                            }
                                            if (!isConsist) {
                                                listOfOrderMovesOfPlayers.addLast(player);
                                            }
                                        }
                                    }
                                } else {

                                    TrooperType trooperType1;
                                    int i = listOfOrderMovesOfTroopers.indexOf(TrooperType.SNIPER);

                                    if (i == listOfOrderMovesOfTroopers.size() - 1) {
                                        i = 0;
                                    } else {
                                        i = i + 1;
                                    }

                                    trooperType1 = listOfOrderMovesOfTroopers.get(i);

                                    if (self.getType() == trooperType1 && scoreDifference >= 0) {
                                        for (TrooperStance trooperStance : TrooperStance.values()) {
                                            int damage = getDamageTrooperInStance(self.getType(), trooperStance);
                                            if (scoreDifference < 145 && scoreDifference != 60 && scoreDifference != 80 && scoreDifference != 85 && scoreDifference != 105 && scoreDifference != 120 && scoreDifference != 140 && (scoreDifference % damage == 0 || (scoreDifference < damage) ? false : (scoreDifference - 25) % damage == 0 || (scoreDifference < damage) ? false : (scoreDifference - 50) % damage == 0)) {
                                                boolean isConsist = false;
                                                for (Player player2 : listOfOrderMovesOfPlayers) {
                                                    if (player2.getId() == player.getId()) {
                                                        isConsist = true;
                                                    }
                                                }
                                                if (!isConsist) {
                                                    listOfOrderMovesOfPlayers.addFirst(player);
                                                }
                                            }
                                        }
                                    }
                                }

                                // ... по медику
                                if (self.getType() == TrooperType.FIELD_MEDIC) {
                                    if (scoreDifference < 0 && scoreDifference != -30 && scoreDifference != -50 && (scoreDifference % 3 == 0 || scoreDifference % 5 == 0)) {
                                        boolean isConsist = false;
                                        for (Player player2 : listOfOrderMovesOfPlayers) {
                                            if (player2.getId() == player.getId()) {
                                                isConsist = true;
                                            }
                                        }
                                        if (!isConsist) {
                                            listOfOrderMovesOfPlayers.addLast(player);
                                        }
                                    } else if(scoreDifference >= 0 ) {
                                        for (TrooperStance trooperStance : TrooperStance.values()) {
                                            int damage = getDamageTrooperInStance(self.getType(), trooperStance);
                                            if (scoreDifference < 105 && scoreDifference != 60 && scoreDifference != 80 && (scoreDifference % damage == 0 || (scoreDifference < damage) ? false : scoreDifference - 25 <= 0 ? false : (scoreDifference - 25) % damage == 0 || (scoreDifference < damage) ? false : scoreDifference - 50 <= 5 ? false : (scoreDifference - 50) % damage == 0)) {
                                                boolean isConsist = false;
                                                for (Player player2 : listOfOrderMovesOfPlayers) {
                                                    if (player2.getId() == player.getId()) {
                                                        isConsist = true;
                                                    }
                                                }
                                                if (!isConsist) {
                                                    listOfOrderMovesOfPlayers.addLast(player);
                                                }
                                            }
                                        }
                                    }
                                } else {

                                    TrooperType trooperType1;
                                    int i = listOfOrderMovesOfTroopers.indexOf(TrooperType.FIELD_MEDIC);

                                    if (i == listOfOrderMovesOfTroopers.size() - 1) {
                                        i = 0;
                                    } else {
                                        i = i + 1;
                                    }

                                    trooperType1 = listOfOrderMovesOfTroopers.get(i);

                                    if (self.getType() == trooperType1) {
                                        if (scoreDifference < 0 && scoreDifference != -30 && scoreDifference != -50 && (scoreDifference % 3 == 0 || scoreDifference % 5 == 0)) {
                                            boolean isConsist = false;
                                            for (Player player2 : listOfOrderMovesOfPlayers) {
                                                if (player2.getId() == player.getId()) {
                                                    isConsist = true;
                                                }
                                            }
                                            if (!isConsist) {
                                                listOfOrderMovesOfPlayers.addFirst(player);
                                            }
                                        } else if(scoreDifference >= 0 ) {
                                            for (TrooperStance trooperStance : TrooperStance.values()) {
                                                int damage = getDamageTrooperInStance(self.getType(), trooperStance);
                                                if (scoreDifference < 105 && scoreDifference != 60 && scoreDifference != 80 && (scoreDifference % damage == 0 || (scoreDifference < damage) ? false : scoreDifference - 25 <= 0 ? false : (scoreDifference - 25) % damage == 0 || (scoreDifference < damage) ? false : scoreDifference - 50 <= 5 ? false : (scoreDifference - 50) % damage == 0)) {
                                                    boolean isConsist = false;
                                                    for (Player player2 : listOfOrderMovesOfPlayers) {
                                                        if (player2.getId() == player.getId()) {
                                                            isConsist = true;
                                                        }
                                                    }
                                                    if (!isConsist) {
                                                        listOfOrderMovesOfPlayers.addFirst(player);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // ... по командиру
                                if (self.getType() == TrooperType.COMMANDER && scoreDifference >= 0) {
                                    for (TrooperStance trooperStance : TrooperStance.values()) {
                                        int damage = getDamageTrooperInStance(self.getType(), trooperStance);
                                        if (scoreDifference <= 150 && scoreDifference != 60 && scoreDifference != 80 && scoreDifference != 85 && scoreDifference != 105 && scoreDifference != 120 && scoreDifference != 140 && scoreDifference != 145 && (scoreDifference % damage == 0 || (scoreDifference < damage) ? false : scoreDifference - 25 <= 0 ? false : (scoreDifference - 25) % damage == 0 || (scoreDifference < damage) ? false : scoreDifference - 50 <= 5 ? false : (scoreDifference - 50) % damage == 0)) {
                                            boolean isConsist = false;
                                            for (Player player2 : listOfOrderMovesOfPlayers) {
                                                if (player2.getId() == player.getId()) {
                                                    isConsist = true;
                                                }
                                            }
                                            if (!isConsist) {
                                                listOfOrderMovesOfPlayers.addLast(player);
                                            }
                                        }
                                    }
                                } else {

                                    TrooperType trooperType1;
                                    int i = listOfOrderMovesOfTroopers.indexOf(TrooperType.COMMANDER);

                                    if (i == listOfOrderMovesOfTroopers.size() - 1) {
                                        i = 0;
                                    } else {
                                        i = i + 1;
                                    }

                                    trooperType1 = listOfOrderMovesOfTroopers.get(i);

                                    if (self.getType() == trooperType1 && scoreDifference >= 0) {
                                        for (TrooperStance trooperStance : TrooperStance.values()) {
                                            int damage = getDamageTrooperInStance(self.getType(), trooperStance);
                                            if (scoreDifference <= 150 && scoreDifference != 60 && scoreDifference != 80 && scoreDifference != 85 && scoreDifference != 105 && scoreDifference != 120 && scoreDifference != 140 && scoreDifference != 145 && (scoreDifference % damage == 0 || (scoreDifference < damage) ? false : scoreDifference - 25 <= 0 ? false : (scoreDifference - 25) % damage == 0 || (scoreDifference < damage) ? false : scoreDifference - 50 <= 5 ? false : (scoreDifference - 50) % damage == 0)) {
                                                boolean isConsist = false;
                                                for (Player player2 : listOfOrderMovesOfPlayers) {
                                                    if (player2.getId() == player.getId()) {
                                                        isConsist = true;
                                                    }
                                                }
                                                if (!isConsist) {
                                                    listOfOrderMovesOfPlayers.addLast(player);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                listOfOrderMovesOfPlayersTemp = players;
            }

            if (listOfOrderMovesOfPlayers.size() == hpOfTroopers.length) {
                isOrder = true;
            }
        }
    }

    // проверяет видна ли клетка врагом (учитывается listOfEnemyTroopers), есил нет, то проверяются ХП юнита, если хп уменьшилось, значит клетка опасна и юнит пытается найти другую клетку более безопасную и уйти в неё
    boolean whenHpOfTrooperIsChanged (Trooper self) {

        boolean isThinkingUnitHideButItIsLie = true;
        for (Trooper trooper : listOfEnemyTroopers) {
            if (world.isVisible(trooper.getVisionRange(), trooper.getX(), trooper.getY(), TrooperStance.STANDING, self.getX(), self.getY(), self.getStance())) {
                isThinkingUnitHideButItIsLie = false;
            }
        }

        if(isThinkingUnitHideButItIsLie) {
            for (int i = 0; i < hpOfTroopers.length; i++) {
                if (hpOfTroopers[i][0] == self.getId() && self.getHitpoints() < hpOfTroopers[i][1]) {
                    boolean flag = true;
                    for (thePoint point : listOfStoredCells) {
                        if (point.getX() == self.getX() && point.getY() == self.getY()) {
                            flag = false;
                        }
                    }
                    if(flag) {
                        listOfStoredCells.add(new thePoint(self.getX(), self.getY()));
                        return true;
                    }
                }
            }
        }
        return false;
    }

    thePoint freeCell(int x, int y, LinkedList<thePoint> listOfBadPoints) {
        int x1 = x - 1;
        int y1 = y - 1;
        int x2 = x + 1;
        int y2 = y + 1;

        for (int i = x1; i <= x2; i++) {
            for (int j = y1; j <= y2; j++) {
                if (i < world.getWidth() && i >= 0 && j < world.getHeight() && j >= 0 && getDistancePointToPoint(x, y, i, j) == 1) {

                    boolean flag = true;
                    for (thePoint point : listOfBadPoints) {
                        if (point.getX() == i && point.getY() == j) {
                            flag = false;
                        }
                    }

                    if (flag) {
                        return new thePoint(i, j);
                    }

                }
            }
        }

        return null;
    }

    boolean followAfterTrooper(Trooper self, int targetX, int targetY) {

        LinkedList<thePoint> path1 = lee(self, self.getX(), self.getY(), targetX, targetY, false);
        LinkedList<thePoint> path2 = null;

        Trooper tempTrooper;

        if (indexOfScout != -1) {
            tempTrooper = troopers[indexOfScout];
            path2 = lee(self, self.getX(), self.getY(), tempTrooper.getX(), tempTrooper.getY(), false);
            if(path2 != null && path2.size() > 1 && path2.size() <= 7) {
                path2 = null;
                path2 = lee(tempTrooper, tempTrooper.getX(), tempTrooper.getY(), targetX, targetY, false);

                if (testOnTrueMap(self, path1)) {
                    return true;
                }

                if (path1 != null && path2 != null && path2.size() - path1.size() >= 0) {

                    if (complatedPathOfTrooper != null && complatedPathOfTrooper.size() > 1 && trueMapOfPoints[complatedPathOfTrooper.get(complatedPathOfTrooper.size() - 1).getX()][complatedPathOfTrooper.get(complatedPathOfTrooper.size() - 1).getY()] > 4) {

                        if (!isUseLastMove && goOnPath(self, complatedPathOfTrooper.get(complatedPathOfTrooper.size() - 1).getX(), complatedPathOfTrooper.get(complatedPathOfTrooper.size() - 1).getY(), true)) {
                            isUseLastMove = true;
                            complatedPathOfTrooper.remove(complatedPathOfTrooper.size() - 1);
                            complatedPathOfTrooper.remove(complatedPathOfTrooper.size() - 1);
                            return true;
                        } else {
                            isUseLastMove = false;
                            move.setAction(ActionType.END_TURN);
                            return true;
                        }

                    } else {

                        if (trueMapOfPoints[self.getX()][self.getY()] > 4) {
                            move.setAction(ActionType.END_TURN);
                            return true;
                        } else {
                            for (int i = self.getX() - 1; i <= self.getX() + 1; i++) {
                                for (int j = self.getY() - 1; j <= self.getY() + 1; j++) {
                                    if (self.getDistanceTo(i, j) <= 1 && trueMapOfPoints[i][j] > trueMapOfPoints[self.getX()][self.getY()]) {
                                        if (goOnPath(self, i, j, true)) {
                                            return true;
                                        }
                                    }
                                }
                            }
                            for (int i = self.getX() - 1; i <= self.getX() + 1; i++) {
                                for (int j = self.getY() - 1; j <= self.getY() + 1; j++) {
                                    if (self.getDistanceTo(i, j) <= 1 && trueMapOfPoints[i][j] >= trueMapOfPoints[self.getX()][self.getY()]) {
                                        if (goOnPath(self, i, j, true)) {
                                            return true;
                                        }
                                    }
                                }
                            }

                            move.setAction(ActionType.END_TURN);
                            return true;
                        }

                    }

                } else if (isUseLastMove) {

                    isUseLastMove = false;
                    move.setAction(ActionType.END_TURN);
                    return true;

                }
            }

        } else if (self.getType() != TrooperType.COMMANDER) {

            if (indexOfCommander != -1) {

                tempTrooper = troopers[indexOfCommander];
                path2 = lee(self, self.getX(), self.getY(), tempTrooper.getX(), tempTrooper.getY(), false);
                if(path2 != null && path2.size() > 1 && path2.size() <= 7) {
                    path2 = null;
                    path2 = lee(tempTrooper, tempTrooper.getX(), tempTrooper.getY(), targetX, targetY, false);

                    if (testOnTrueMap(self, path1)) {
                        return true;
                    }

                    if (path1 != null && path2 != null && path2.size() - path1.size() >= 0) {

                        if (complatedPathOfTrooper != null && complatedPathOfTrooper.size() > 1 && trueMapOfPoints[complatedPathOfTrooper.get(complatedPathOfTrooper.size() - 1).getX()][complatedPathOfTrooper.get(complatedPathOfTrooper.size() - 1).getY()] > 4) {

                            if (!isUseLastMove && goOnPath(self, complatedPathOfTrooper.get(complatedPathOfTrooper.size() - 1).getX(), complatedPathOfTrooper.get(complatedPathOfTrooper.size() - 1).getY(), true)) {
                                isUseLastMove = true;
                                complatedPathOfTrooper.remove(complatedPathOfTrooper.size() - 1);
                                complatedPathOfTrooper.remove(complatedPathOfTrooper.size() - 1);
                                return true;
                            } else {
                                isUseLastMove = false;
                                move.setAction(ActionType.END_TURN);
                                return true;
                            }

                        } else {

                            if (trueMapOfPoints[self.getX()][self.getY()] > 4) {
                                move.setAction(ActionType.END_TURN);
                                return true;
                            } else {
                                for (int i = self.getX() - 1; i <= self.getX() + 1; i++) {
                                    for (int j = self.getY() - 1; j <= self.getY() + 1; j++) {
                                        if (self.getDistanceTo(i, j) <= 1 && trueMapOfPoints[i][j] > trueMapOfPoints[self.getX()][self.getY()]) {
                                            if (goOnPath(self, i, j, true)) {
                                                return true;
                                            }
                                        }
                                    }
                                }
                                for (int i = self.getX() - 1; i <= self.getX() + 1; i++) {
                                    for (int j = self.getY() - 1; j <= self.getY() + 1; j++) {
                                        if (self.getDistanceTo(i, j) <= 1 && trueMapOfPoints[i][j] >= trueMapOfPoints[self.getX()][self.getY()]) {
                                            if (goOnPath(self, i, j, true)) {
                                                return true;
                                            }
                                        }
                                    }
                                }

                                move.setAction(ActionType.END_TURN);
                                return true;
                            }

                        }

                    } else if (isUseLastMove) {

                        isUseLastMove = false;
                        move.setAction(ActionType.END_TURN);
                        return true;

                    }
                }

            } else if (self.getType() != TrooperType.SOLDIER) {

                if (indexOfSoldier != -1) {

                    tempTrooper = troopers[indexOfSoldier];
                    path2 = lee(self, self.getX(), self.getY(), tempTrooper.getX(), tempTrooper.getY(), false);
                    if(path2 != null && path2.size() > 1 && path2.size() <= 7) {
                        path2 = null;
                        path2 = lee(tempTrooper, tempTrooper.getX(), tempTrooper.getY(), targetX, targetY, false);

                        if (testOnTrueMap(self, path1)) {
                            return true;
                        }

                        if (path1 != null && path2 != null && path2.size() - path1.size() >= 0) {

                            if (complatedPathOfTrooper != null && complatedPathOfTrooper.size() > 1 && trueMapOfPoints[complatedPathOfTrooper.get(complatedPathOfTrooper.size() - 1).getX()][complatedPathOfTrooper.get(complatedPathOfTrooper.size() - 1).getY()] > 4) {

                                if (!isUseLastMove && goOnPath(self, complatedPathOfTrooper.get(complatedPathOfTrooper.size() - 1).getX(), complatedPathOfTrooper.get(complatedPathOfTrooper.size() - 1).getY(), true)) {
                                    isUseLastMove = true;
                                    complatedPathOfTrooper.remove(complatedPathOfTrooper.size() - 1);
                                    complatedPathOfTrooper.remove(complatedPathOfTrooper.size() - 1);
                                    return true;
                                } else {
                                    isUseLastMove = false;
                                    move.setAction(ActionType.END_TURN);
                                    return true;
                                }

                            } else {

                                if (trueMapOfPoints[self.getX()][self.getY()] > 4) {
                                    move.setAction(ActionType.END_TURN);
                                    return true;
                                } else {
                                    for (int i = self.getX() - 1; i <= self.getX() + 1; i++) {
                                        for (int j = self.getY() - 1; j <= self.getY() + 1; j++) {
                                            if (self.getDistanceTo(i, j) <= 1 && trueMapOfPoints[i][j] > trueMapOfPoints[self.getX()][self.getY()]) {
                                                if (goOnPath(self, i, j, true)) {
                                                    return true;
                                                }
                                            }
                                        }
                                    }
                                    for (int i = self.getX() - 1; i <= self.getX() + 1; i++) {
                                        for (int j = self.getY() - 1; j <= self.getY() + 1; j++) {
                                            if (self.getDistanceTo(i, j) <= 1 && trueMapOfPoints[i][j] >= trueMapOfPoints[self.getX()][self.getY()]) {
                                                if (goOnPath(self, i, j, true)) {
                                                    return true;
                                                }
                                            }
                                        }
                                    }

                                    move.setAction(ActionType.END_TURN);
                                    return true;
                                }

                            }

                        } else if (isUseLastMove) {

                            isUseLastMove = false;
                            move.setAction(ActionType.END_TURN);
                            return true;

                        }
                    }

                } else if (self.getType() != TrooperType.FIELD_MEDIC) {

                    if (indexOfMedic != -1) {

                        tempTrooper = troopers[indexOfMedic];
                        path2 = lee(self, self.getX(), self.getY(), tempTrooper.getX(), tempTrooper.getY(), false);
                        if(path2 != null && path2.size() > 1 && path2.size() <= 7) {
                            path2 = null;
                            path2 = lee(tempTrooper, tempTrooper.getX(), tempTrooper.getY(), targetX, targetY, false);

                            if (testOnTrueMap(self, path1)) {
                                return true;
                            }

                            if (path1 != null && path2 != null && path2.size() - path1.size() >= 0) {

                                if (complatedPathOfTrooper != null && complatedPathOfTrooper.size() > 1 && trueMapOfPoints[complatedPathOfTrooper.get(complatedPathOfTrooper.size() - 1).getX()][complatedPathOfTrooper.get(complatedPathOfTrooper.size() - 1).getY()] > 4) {

                                    if (!isUseLastMove && goOnPath(self, complatedPathOfTrooper.get(complatedPathOfTrooper.size() - 1).getX(), complatedPathOfTrooper.get(complatedPathOfTrooper.size() - 1).getY(), true)) {
                                        isUseLastMove = true;
                                        complatedPathOfTrooper.remove(complatedPathOfTrooper.size() - 1);
                                        complatedPathOfTrooper.remove(complatedPathOfTrooper.size() - 1);
                                        return true;
                                    } else {
                                        isUseLastMove = false;
                                        move.setAction(ActionType.END_TURN);
                                        return true;
                                    }

                                } else {

                                    if (trueMapOfPoints[self.getX()][self.getY()] > 4) {
                                        move.setAction(ActionType.END_TURN);
                                        return true;
                                    } else {
                                        for (int i = self.getX() - 1; i <= self.getX() + 1; i++) {
                                            for (int j = self.getY() - 1; j <= self.getY() + 1; j++) {
                                                if (self.getDistanceTo(i, j) <= 1 && trueMapOfPoints[i][j] > trueMapOfPoints[self.getX()][self.getY()]) {
                                                    if (goOnPath(self, i, j, true)) {
                                                        return true;
                                                    }
                                                }
                                            }
                                        }
                                        for (int i = self.getX() - 1; i <= self.getX() + 1; i++) {
                                            for (int j = self.getY() - 1; j <= self.getY() + 1; j++) {
                                                if (self.getDistanceTo(i, j) <= 1 && trueMapOfPoints[i][j] >= trueMapOfPoints[self.getX()][self.getY()]) {
                                                    if (goOnPath(self, i, j, true)) {
                                                        return true;
                                                    }
                                                }
                                            }
                                        }

                                        move.setAction(ActionType.END_TURN);
                                        return true;
                                    }

                                }

                            } else if (isUseLastMove) {

                                isUseLastMove = false;
                                move.setAction(ActionType.END_TURN);
                                return true;

                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    boolean testSniperShootingThenMoveDown(Trooper self, Trooper target) {

        if(self.getType() == TrooperType.SNIPER && self.getActionPoints() >= self.getShootCost() + game.getStanceChangeCost() && !world.isVisible(self.getShootingRange(), self.getX(), self.getY(), self.getStance(), target.getX(), target.getY(), target.getStance())) {

            if (self.getDistanceTo(target) <= self.getShootingRange() + 1 && self.getStance() == TrooperStance.STANDING || self.getDistanceTo(target) <= self.getShootingRange() + 1 && self.getStance() == TrooperStance.KNEELING) {

                if (world.isVisible(self.getShootingRange() + 1, self.getX(), self.getY(), self.getStance(), target.getX(), target.getY(), target.getStance())) {
                    move.setAction(ActionType.LOWER_STANCE);
                    return true;
                }

            }

        } else if (self.getType() == TrooperType.SNIPER && self.getActionPoints() < self.getShootCost()) {

            if (self.getActionPoints() >= game.getStanceChangeCost() && self.getDistanceTo(target) <= self.getShootingRange() + 1 && self.getStance() == TrooperStance.STANDING && world.isVisible(self.getShootingRange() + 1, self.getX(), self.getY(), TrooperStance.KNEELING, target.getX(), target.getY(), target.getStance()) || self.getActionPoints() >= game.getStanceChangeCost() && self.getDistanceTo(target) <= self.getShootingRange() + 1 && self.getStance() == TrooperStance.KNEELING && world.isVisible(self.getShootingRange() + 1, self.getX(), self.getY(), TrooperStance.PRONE, target.getX(), target.getY(), target.getStance())) {
                move.setAction(ActionType.LOWER_STANCE);
                return true;
            } else if (self.getDistanceTo(target) <= self.getShootingRange() + 2 && self.getStance() == TrooperStance.STANDING && self.getActionPoints() >= 4) {
                if (world.isVisible(self.getShootingRange() + 2, self.getX(), self.getY(), TrooperStance.PRONE, target.getX(), target.getY(), target.getStance())) {
                    move.setAction(ActionType.LOWER_STANCE);
                    return true;
                }
            }
        }

        return false;
    }

    boolean testOnTrueMap(Trooper self, LinkedList<MyStrategy.thePoint> path1) {
        if (self.getActionPoints() < (clearSelfArea(self, 1, 1) ? game.getStandingMoveCost() * 3 : game.getStandingMoveCost() * 4)) {

            if (trueMapOfPoints[self.getX()][self.getY()] < 4) {

                if (path1 != null && path1.size() > 1 && trueMapOfPoints[path1.get(1).getX()][path1.get(1).getY()] > 2) {
                    return false;
                }

                if (complatedPathOfTrooper != null && complatedPathOfTrooper.size() != 0 && trueMapOfPoints[complatedPathOfTrooper.get(complatedPathOfTrooper.size() - 1).getX()][complatedPathOfTrooper.get(complatedPathOfTrooper.size() - 1).getY()] > 4) {
                    move.setAction(ActionType.MOVE);
                    move.setX(complatedPathOfTrooper.get(complatedPathOfTrooper.size() - 1).getX());
                    move.setY(complatedPathOfTrooper.get(complatedPathOfTrooper.size() - 1).getY());
                    complatedPathOfTrooper.remove(complatedPathOfTrooper.size() - 1);
                    return true;
                }
            }

            if (listOfEnemyTroopers.size() == 0 && forwardTrooper != -1 && self.getType() != troopers[forwardTrooper].getType()) {
                int targx, targy;
                if (localTargetX != 100) {
                    targx = localTargetX;
                    targy = localTargetY;
                } else {
                    targx = globalTargetX;
                    targy = globalTargetY;
                }
                LinkedList<thePoint> tempPath1 = lee(troopers[forwardTrooper], troopers[forwardTrooper].getX(), troopers[forwardTrooper].getY(), targx, targy, true);
                LinkedList<thePoint> tempPath2 = lee(troopers[forwardTrooper], troopers[forwardTrooper].getX(), troopers[forwardTrooper].getY(), targx, targy, false);
                if (tempPath1 == null && tempPath2 != null || tempPath1 != null && tempPath1.size() > 1 && tempPath1.size() > tempPath2.size() + 5) {
                    thePoint nearPoint = findAnyCell(self);
                    if (nearPoint != null) {
                        isGoNear = true;
                        idOfTrooperStop = (int) self.getId();
                        if (goOnPath(self, nearPoint.getX(), nearPoint.getY(), true)) {
                            return true;
                        }
                    }
                }
            }

            if (listOfEnemyTroopers.size() == 0 && path1 != null && path1.size() > 1 && ((trueMapOfPoints[path1.get(1).getX()][path1.get(1).getY()] < 4) || trueMapOfPoints[self.getX()][self.getY()] > trueMapOfPoints[path1.get(1).getX()][path1.get(1).getY()])) {
                isUseLastMove = false;
                move.setAction(ActionType.END_TURN);
                return true;
            }
        } else {
            if (listOfEnemyTroopers.size() == 0 && forwardTrooper != -1 && self.getType() != troopers[forwardTrooper].getType()) {
                int targx, targy;
                if (localTargetX != 100) {
                    targx = localTargetX;
                    targy = localTargetY;
                } else {
                    targx = globalTargetX;
                    targy = globalTargetY;
                }
                LinkedList<thePoint> tempPath1 = lee(troopers[forwardTrooper], troopers[forwardTrooper].getX(), troopers[forwardTrooper].getY(), targx, targy, true);
                LinkedList<thePoint> tempPath2 = lee(troopers[forwardTrooper], troopers[forwardTrooper].getX(), troopers[forwardTrooper].getY(), targx, targy, false);
                if (tempPath1 == null && tempPath2 != null || tempPath1 != null && tempPath1.size() > 1 && tempPath1.size() > tempPath2.size() + 3) {
                      thePoint nearPoint = findAnyCell(self);
                    if (nearPoint != null) {
                        isGoNear = true;
                        idOfTrooperStop = (int) self.getId();
                        if (goOnPath(self, nearPoint.getX(), nearPoint.getY(), true)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    // для вычисления урона заданного типа юнита в заданной стойке, возможно понадобиться при подсчёте всех живых юнитов в игре
    int getDamageTrooperInStance(TrooperType trooperType, TrooperStance trooperStance) {
        switch (trooperType) {

            case FIELD_MEDIC: {

                switch (trooperStance) {

                    case STANDING: return 9;

                    case KNEELING: return 12;

                    case PRONE: return 15;
                }

                break;
            }

            case COMMANDER: {

                switch (trooperStance) {

                    case STANDING: return 15;

                    case KNEELING: return 20;

                    case PRONE: return 25;
                }

                break;
            }

            case SOLDIER: {

                switch (trooperStance) {

                    case STANDING: return 25;

                    case KNEELING: return 30;

                    case PRONE: return 35;
                }

                break;
            }

            case SNIPER: {

                switch (trooperStance) {

                    case STANDING: return 65;

                    case KNEELING: return 80;

                    case PRONE: return 95;
                }

                break;
            }

            case SCOUT: {

                switch (trooperStance) {

                    case STANDING: return 20;

                    case KNEELING: return 25;

                    case PRONE: return 30;
                }

                break;
            }
        }

        return 0;
    }

    private class GameUnit {
        Trooper trooper;
        int worldMove;
        boolean isNotInFog;

        GameUnit(Trooper troop) {
            trooper = troop;
            this.worldMove = world.getMoveIndex();
            for (Trooper trooper1 : listOfEnemys) {
                if (trooper1.getId() == trooper.getId()) {
                    this.isNotInFog = true;
                    break;
                }
            }
        }
    }

    private class ListOfPlayers {
        Player player;
        boolean isDead;
        int approximateX = -1;
        int approximateY = -1;

        ListOfPlayers(Player player) {
            this.player = player;
            this.isDead = false;
        }
    }

    private class thePoint {
        int x;
        int y;
        int indexOfTailTime = 0;
        int worldMove = world.getMoveIndex();

        thePoint(int x, int y) {
            this.x = x;
            this.y = y;
        }

        int getX() {
            return x;
        }

        int getY() {
            return y;
        }
    }
}