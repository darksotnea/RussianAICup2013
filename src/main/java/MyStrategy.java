import model.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Random;

import static java.lang.StrictMath.hypot;

public final class MyStrategy implements Strategy {
    private final int HP_WHEN_HEAL = 99;
    private final int HP_WHEN_GO_MEDIC = 85;
    private final int AREA_OF_COMMANDER = 4;
    private final int AREA_OF_SOLDIER = 5;
    private final int AREA_OF_SNIPER = 6;
    private final int AREA_OF_GRENADE = 5;
    private final int ACTION_POINT_OF_GRENADE_THROW = 8;
    private final int ACTION_POINT_OF_MEDIKIT_USE = 2;
    private final int ACTION_POINT_OF_FIELD_RATION_EAT = 2;
    private final int MIN_DISTANCE_FOR_LOCALTARGET = 1;
    private final int TIME_EXPIRE_OF_LISTOFSOWENEMYS = 1;
    private static boolean beginBattle = false;
    private static thePoint safePoint;
    private static boolean goToSafePlace = false;
    private static int globalTargetX = -1;
    private static int globalTargetY = -1;
    private static int lastMoveX = 0;
    private static int lastMoveY = 0;
    private static int localTargetX = 100;
    private static int localTargetY = 100;
    private static boolean detectEnemyByTeam = false;
    private static boolean getHelpFromAir = false;
    private static boolean needHelpFromAir = false;
    private static boolean istroopersUnderAttack = false;
    private static int trooperUnderAttack;
    private static int numOfTroopers = 0;
    private static int teamSupportCount;
    private int indexOfCommander;
    private int indexOfMedic;
    private int indexOfSoldier;
    private int indexOfSniper;
    private int indexOfScout;
    private final Random random = new Random();
    private static Trooper targetTrooper = null;
    private static LinkedList<Trooper> listOfEnemys;
    private static int[][] hpOfTroopers;
    private static LinkedList<Integer> remainingQuarters = new LinkedList<>();
    private static int[][] cellsInt;
    private static int[][] trueMapOfPoints;
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
    private static boolean isEnemyStrategyCrashed = false;
    private boolean goToBonus = false;
    private static int myScore = 0;
    private static boolean isShootingAnywhere = false;
    private static int targetUnitIdSave = -1;
    private static int idOfTrooperStop = -1;
    private static boolean isThrowGrenadeOnSowTroopers = false;
    private static boolean isUseLastMove = false;

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

        //строим карту с приоритетом ячеек
        trueMapOfPoints = getMapOfPoints(self);

        if (self.getId() == idOfTrooperStop) {
            idOfTrooperStop = -1;
            move.setAction(ActionType.END_TURN);
            return;
        }

        if (targetTrooper == null) {
            beginBattle = false;
        }

        if (detectEnemyByTeam == true && targetTrooper == null && localTargetX == 100) {
            detectEnemyByTeam = false;
        }

        if (!(saveMoveWorld == world.getMoveIndex() || saveMoveWorld == world.getMoveIndex() - 1)) {
            istroopersUnderAttack = false;
        }

        if (goToSafePlace) {
            saveMoveSafePlace = world.getMoveIndex();
            savedTrooperId = (int) self.getId();
        } else if (saveMoveSafePlace == world.getMoveIndex() && savedTrooperId == self.getId()){
            move.setAction(ActionType.END_TURN);
            return;
        }

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

        if (isShootingAnywhere) {
            isShootingAnywhere = false;
            for (Player player : world.getPlayers()) {
                if (player.getName().equalsIgnoreCase("darkstone")) {
                    for (GameUnit unit : listOfSowEnemys) {
                        if (unit.trooper.getId() == targetUnitIdSave && myScore > player.getScore()) {
                            unit.worldMove = world.getMoveIndex();
                            break;
                        } else if (unit.trooper.getId() == targetUnitIdSave) {
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

            for (Trooper trooper : troopers) {
                if (!trooper.isTeammate()) {

                    boolean notHere = true;

                    //проверяем на соответствие списка listOfSow живым враженскию юнитам, если соответствует, то обновляем worldMove
                    for (GameUnit gameUnit : listOfSowEnemys) {
                        if (gameUnit.trooper.getId() == trooper.getId() && (gameUnit.trooper.getX() != trooper.getX() || gameUnit.trooper.getY() != trooper.getY())) {
                            gameUnit.trooper = trooper;
                            gameUnit.worldMove = world.getMoveIndex();
                            notHere = false;
                            break;
                        } else if (gameUnit.trooper.getId() == trooper.getId()) {
                            gameUnit.worldMove = world.getMoveIndex();
                            notHere = false;
                        }
                    }

                    //если юнита нету в списке listOfSow, то добавляем его туда
                    if(notHere) {
                        listOfSowEnemys.add(new GameUnit(trooper));
                    }
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

                    boolean isOldOutdated = true;
                    int count = 0;

                    for (Trooper trooper1 : troopers) {
                        if (trooper1.isTeammate() && world.isVisible(trooper1.getVisionRange(), trooper1.getX(), trooper1.getY(), trooper1.getStance(), gameUnit.trooper.getX(), gameUnit.trooper.getY(), gameUnit.trooper.getStance())) {
                            for (Trooper trooper : listOfEnemys) {
                                if (trooper.getX() == gameUnit.trooper.getX() && trooper.getY() == gameUnit.trooper.getY()) {
                                    isOldOutdated = false;
                                    break;
                                }
                            }
                        } else {
                            if(trooper1.isTeammate()) {
                                count++;
                            }
                        }
                        if(!isOldOutdated) {
                            break;
                        }
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
                    listOfSowEnemys.add(new GameUnit(trooper));
                }
            }
        }

//        Рандомизация обхода карты, оринентирована по углам.
        makeATarget(self);

        //реагирование на crash стратегии для игры 2х5
        if (hpOfTroopers.length == 5) {
            Player[] playersTest = world.getPlayers();
            for (Player player : playersTest) {
                if (!player.getName().equalsIgnoreCase("darkstone") && player.isStrategyCrashed()) {
                    if(indexOfCommander != -1) {
                        needHelpFromAir = true;
                    }
                }
            }
        }

        //расчёт поддержки при атаке targetTrooper-a
        teamSupportCount = 0;
        for (Trooper trooper : troopers) {
            if (trooper.isTeammate() && trooper.getType() != self.getType()) {
                if (targetTrooper != null && canSeeOrCanShoot(trooper, targetTrooper, false /*&& troopers[indexOfMedic].getDistanceTo(targetTrooper) <= self.getDistanceTo(targetTrooper)*/)) {
                    teamSupportCount++;
                }
            }
        }

        if (self.getActionPoints() >= 10) {

            boolean tempValue = hpIsChanged(self);
            if(tempValue) {
                saveMoveWorld = world.getMoveIndex();
                istroopersUnderAttack = tempValue;
            }

            processingHpOfTroopers();
        }

        //TODO   @@@@@@@@@@@@@@@@@@@@@@@         КОМАНДОР             @@@@@@@@@@@@@@@@@@@@@@@@@

        if (self.getType() == TrooperType.COMMANDER) {

            if (underAttack(self)) {
//                goToTheAllegedEnemy();
                return;
            }

            //разведка с воздуха
            if (indexOfCommander != -1 && needHelpFromAir && targetTrooper == null && localTargetX == 100 || world.getMoveIndex() % 10 == 0 && targetTrooper == null && localTargetX == 100 && world.getMoveIndex() != 0) {
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

                if (makeABoom(self)) {
                    return;
                }

                if (tryToUseMedkit(self)) {
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

                if (indexOfMedic != -1 && self.getHitpoints() < HP_WHEN_HEAL && isDistanceEqualOrLessOneTail(self, troopers[indexOfMedic])) {
                    move.setAction(ActionType.END_TURN);
                }
            }
        }


        //TODO   @@@@@@@@@@@@@@@@@@@@@@@         СОЛДАТ             @@@@@@@@@@@@@@@@@@@@@@@@@

        if (self.getType() == TrooperType.SOLDIER) {

            if (underAttack(self)) {
                return;
            }

            if (targetTrooper != null) {

                if (self.getActionPoints() <= 7 && self.isHoldingFieldRation() /*&& !self.isHoldingGrenade()*/ && canSeeOrCanShoot(self, targetTrooper, false)) {
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

                if (indexOfMedic != -1 && self.getHitpoints() < HP_WHEN_HEAL && isDistanceEqualOrLessOneTail(self, troopers[indexOfMedic])) {
                    move.setAction(ActionType.END_TURN);
                }
            }
        }


        //TODO   @@@@@@@@@@@@@@@@@@@@@@@         МЕДИК             @@@@@@@@@@@@@@@@@@@@@@@@@@@@

        if (self.getType() == TrooperType.FIELD_MEDIC) {

            if (underAttack(self)) {
                return;
            }

            if (targetTrooper != null) {

                if (self.getActionPoints() <= 7 && self.isHoldingFieldRation() /*&& !self.isHoldingGrenade()*/ && canSeeOrCanShoot(self, targetTrooper, false)) {
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

                if (self.getActionPoints() == 2) {
                    if (tryToUseMedkit(self)) {
                        return;
                    }
                }

               /* boolean canRun = false;
                for (Trooper trooper : troopers) {
                    if (trooper.isTeammate() && self.getType() != trooper.getType() && trooper.getDistanceTo(targetTrooper) > trooper.getShootingRange() && !canSeeOrCanShoot(trooper, targetTrooper, false)) {
                        canRun = true;
                    }
                }*/
                /*if (medicUnderAttack*//* || teamSupportCount < 2 && canRun*//*) {
                    thePoint point = findNotAchievableTail(self, true);
                    if (point != null) {
                        if (goOnPath(self, point.getX(), point.getY(), true)) {
                            return;
                        }
                    }
                } else {*/
                if (goHeal(self)) {
                    return;
                }
                /*}*/


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


        //TODO   @@@@@@@@@@@@@@@@@@@@@@@        РАЗВЕДЧИК             @@@@@@@@@@@@@@@@@@@@@@@@@@@@
        if (self.getType() == TrooperType.SCOUT) {

            if (underAttack(self)) {
                return;
            }

            if (targetTrooper != null) {

                if (self.getActionPoints() <= 7 && self.isHoldingFieldRation() /*&& !self.isHoldingGrenade()*/ && canSeeOrCanShoot(self, targetTrooper, false)) {
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

                if (indexOfMedic != -1 && self.getHitpoints() < HP_WHEN_HEAL && isDistanceEqualOrLessOneTail(self, troopers[indexOfMedic])) {
                    move.setAction(ActionType.END_TURN);
                }
            }
        }


        //TODO   @@@@@@@@@@@@@@@@@@@@@@@         СНАЙПЕР             @@@@@@@@@@@@@@@@@@@@@@@@@@@@

        if (self.getType() == TrooperType.SNIPER) {

            if (underAttack(self)) {
                return;
            }

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

                if (goToMedic(self)) {
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

                if (indexOfMedic != -1 && self.getHitpoints() < HP_WHEN_HEAL && isDistanceEqualOrLessOneTail(self, troopers[indexOfMedic])) {
                    move.setAction(ActionType.END_TURN);
                }
            }
        }

        //TODO   @@@@@@@@@@@@@@@@@@@@@@@         СТРЕЛЬБА ПО УМОЛЧАНИЮ И ОПРЕДЕЛЕНИЕ ЦЕЛИ            @@@@@@@@@@@@@@@@@@@@@@@@@@@@

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

            //встаём, если нет врагов и если находимся не в стоячей позиции //TODO эксперименты с досягаемостью снайпера, он ползёт к цели если может выстрелить в юнита в состаянии PRONE на этой клетке
            if (listOfSowEnemys.size() == 0 && (self.getStance() != TrooperStance.STANDING && targetTrooper == null && self.getType() != TrooperType.SNIPER || self.getStance() != TrooperStance.STANDING && self.getType() == TrooperType.SNIPER && targetTrooper == null && !(localTargetX != 100 && world.isVisible(self.getShootingRange(), self.getX(), self.getY(), self.getStance(), localTargetX, localTargetY, TrooperStance.PRONE)))) {
                move.setAction(ActionType.RAISE_STANCE);
                return;
            }

            if(listOfSowEnemys.size() != 0 && self.getType() == TrooperType.SNIPER) {
                if(self.getDistanceTo(localTargetX, localTargetY) > self.getShootingRange()) {
                    if (self.getDistanceTo(localTargetX, localTargetY) <= self.getShootingRange() + 2 && self.getStance() == TrooperStance.STANDING || self.getDistanceTo(localTargetX, localTargetY) <= self.getShootingRange() + 1 && self.getStance() == TrooperStance.KNEELING) {
                        for (GameUnit gameUnit : listOfSowEnemys) {
                            if (world.isVisible(self.getShootingRange() + 2, self.getX(), self.getY(), self.getStance(), gameUnit.trooper.getX(), gameUnit.trooper.getY(), gameUnit.trooper.getStance())) {
                                move.setAction(ActionType.LOWER_STANCE);
                                return;
                            }
                        }
                    }
                }
            }
        }

        if (self.getActionPoints() >= getCostMoveWithStance(self)) {
            if (targetX == globalTargetX && targetY == globalTargetY || targetX == localTargetX && targetY == localTargetY) {

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

               /* if(indexOfCommander != -1 && indexOfMedic != -1 && self.getType() == TrooperType.FIELD_MEDIC && self.getDistanceTo(troopers[indexOfCommander]) > 3) {
                    targetX = troopers[indexOfCommander].getX();
                    targetY = troopers[indexOfCommander].getY();
                } else if (indexOfCommander == -1 && indexOfMedic != -1 && indexOfSoldier != -1 && self.getType() == TrooperType.FIELD_MEDIC && self.getDistanceTo(troopers[indexOfSoldier]) > 3) {
                    targetX = troopers[indexOfSoldier].getX();
                    targetY = troopers[indexOfSoldier].getY();
                }

                if(indexOfCommander != -1 && indexOfSniper != -1 && self.getType() == TrooperType.SNIPER && self.getDistanceTo(troopers[indexOfCommander]) > 3) {
                    targetX = troopers[indexOfCommander].getX();
                    targetY = troopers[indexOfCommander].getY();
                } else if (indexOfCommander == -1 && indexOfSniper != -1 && indexOfSoldier != -1 && self.getType() == TrooperType.SNIPER && self.getDistanceTo(troopers[indexOfSoldier]) > 3) {
                    targetX = troopers[indexOfSoldier].getX();
                    targetY = troopers[indexOfSoldier].getY();
                }*/

                /*//проверка на попадание в радиус медика, если выходишь возврат обратно
                if(indexOfMedic != -1 && self.getDistanceTo(troopers[indexOfMedic]) > AREA_OF_MEDIC) {
                    targetX = troopers[indexOfMedic].getX();
                    targetY = troopers[indexOfMedic].getY();
                }*/

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

            //чтобы медик не бежал первым!! так как его обзор очень маленький, первым можно бежать если осталось 2 юнита
            if (indexOfSniper != -1 && indexOfMedic != -1 && (indexOfScout != -1 || indexOfCommander != -1 && indexOfSoldier != -1)) {
                if (self.getType() == TrooperType.FIELD_MEDIC && self.getDistanceTo(troopers[indexOfSniper]) >= 3 && indexOfSoldier != -1 && indexOfCommander != -1 && indexOfScout != -1) {
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

    boolean goOnPath(Trooper self, int targetX, int targetY, boolean isWithTroopers) {
        //отвечает за передвежение по полю из точки где находится юнит в указанную точку, путь выбирается через волновой алгоритм lee.
        if (self.getActionPoints() >= getCostMoveWithStance(self)) {
            LinkedList<thePoint> pathOfTrooper;

            //TODO тестю не убегание медика!!
            if (indexOfSniper != -1 && indexOfMedic != -1) {
                if (self.getType() == TrooperType.FIELD_MEDIC && self.getDistanceTo(troopers[indexOfSniper]) >= 4 && targetHeal == null && !goToBonus && !goToSafePlace) {
                    targetX = troopers[indexOfSniper].getX();
                    targetY = troopers[indexOfSniper].getY();
                }
            }

            if (goToSafePlace) {
                if (self.getStance() != TrooperStance.STANDING) {
                    move.setAction(ActionType.RAISE_STANCE);
                    return true;
                }
                pathOfTrooper = lee(self, self.getX(), self.getY(), safePoint.getX(), safePoint.getY(), true);
            } else {
                if (isWithTroopers) {
                    pathOfTrooper = lee(self, self.getX(), self.getY(), targetX, targetY, true);
                } else {
                    pathOfTrooper = lee(self, self.getX(), self.getY(), targetX, targetY, false);
                }
            }
            if (pathOfTrooper != null && pathOfTrooper.size() != 0 && pathOfTrooper.size() != 1) {

                if (self.getActionPoints() >= game.getStanceChangeCost() && self.getType() != TrooperType.SNIPER && self.getStance() != TrooperStance.STANDING) {
                    move.setAction(ActionType.RAISE_STANCE);
                    return true;
                }

                //избегание плохих позиций, если следующая ячейка в таблице trueMapOfPoints == 2, то тогда если её можно обойти за кол-во ходов текущего пути + 5, идём в обход, если нельзя, то встаём и пробуем пройти уже в положении STANDING.
                if (trueMapOfPoints[pathOfTrooper.get(1).getX()][pathOfTrooper.get(1).getY()] == 2 && self.getActionPoints() < 4 * getCostMoveWithStance(self) && self.getType() != TrooperType.SNIPER && !goToSafePlace && !goThrowGrenade && !goToBonus) {
                    if (self.getActionPoints() >= getCostMoveWithStance(self)) {
                        cellsInt[pathOfTrooper.get(1).getX()][pathOfTrooper.get(1).getY()] = -5;
                        LinkedList<thePoint> tempPath = lee(self, self.getX(), self.getY(), targetX, targetY, true);
                        if (tempPath != null && tempPath.size() != 0 && tempPath.size() != 1 && pathOfTrooper.size() + 5 >= tempPath.size()) {
                            pathOfTrooper = tempPath;
                        } else if (self.getStance() != TrooperStance.STANDING && self.getActionPoints() >= game.getStanceChangeCost() && self.getType() != TrooperType.SNIPER) {
                            move.setAction(ActionType.RAISE_STANCE);
                            return true;
                        } else if (self.getActionPoints() >= game.getStanceChangeCost() && self.getType() == TrooperType.SNIPER && (self.getStance() != TrooperStance.KNEELING || self.getStance() != TrooperStance.STANDING)) {
                            move.setAction(ActionType.RAISE_STANCE);
                            return true;
                        }
                    } else {
                        /*if (self.getStance() != TrooperStance.STANDING && self.getActionPoints() >= game.getStanceChangeCost()) {
                            move.setAction(ActionType.RAISE_STANCE);
                            return true;
                        }*/
                        move.setAction(ActionType.END_TURN);
                        return true;
                    }
                }

                if (trueMapOfPoints[pathOfTrooper.get(1).getX()][pathOfTrooper.get(1).getY()] == 4 && listOfSowEnemys.size() == 0 && self.getActionPoints() >= getCostMoveWithStance(self) && self.getActionPoints() < getCostMoveWithStance(self) * 2 && targetTrooper == null && localTargetX != 100 && !goToSafePlace && !goThrowGrenade && !goToBonus) {
                    move.setAction(ActionType.END_TURN);
                    return true;
                }

                move.setAction(ActionType.MOVE);
                move.setX(pathOfTrooper.get(1).getX());
                move.setY(pathOfTrooper.get(1).getY());

                if (!testTail(self, pathOfTrooper, targetX, targetY)) {
                    return false;
                }

                if (isBetweenWalls(self.getX(), self.getY())) {
                    for (Trooper trooper : troopers) {
                        if (trooper.isTeammate() && move.getX() == trooper.getX() && move.getY() == trooper.getY()) {
                            if (goOnPath(self, globalTargetX, globalTargetY, false)) {
                                return true;
                            }
                        }
                    }
                }

                if (safePoint != null && pathOfTrooper.get(1).getX() == safePoint.getX() && pathOfTrooper.get(1).getY() == safePoint.getY()) {
                    goToSafePlace = false;
                    safePoint = null;
                    idOfTrooperStop = (int) self.getId();
                }

                if (!(self.getX() == move.getX() && self.getY() == move.getY())) {
                    lastMoveX = self.getX();
                    lastMoveY = self.getY();
                    return true;
                }
            }
        }
        return false;
    }

    //когда обнаружены чужие юниты, то начинает подходить, стрелять, отходить, контролирует параметры очков хода
    boolean goOnWar(Trooper self, int targetX, int targetY) {

        //медик при обнаружении врага пытается его добить если мало хп и бежит к дальнему своему юниту.
        if (self.getType() == TrooperType.FIELD_MEDIC && listOfEnemys != null && listOfEnemys.size() != 0) {

            //пытается убить любую вражескую цель, если она убиваема
            if (killAnyEnemyUnit(self)) {
                return true;
            }

            if (indexOfSniper != -1 && self.getDistanceTo(troopers[indexOfSniper]) > 3) {
                if (targetTrooper!= null && canSeeOrCanShoot(self, targetTrooper, false)) {
                    if (troopers[indexOfSniper].getHitpoints() <= 30) {
                        if (goOnPath(self, troopers[indexOfSniper].getX(), troopers[indexOfSniper].getY(), false)) {
                            return true;
                        }
                    } else {
                        if (canShootOnTarget(self, targetTrooper)) {
                            shootOnTarget(self, targetTrooper);
                            return true;
                        }
                    }
                }

                if (goOnPath(self, troopers[indexOfSniper].getX(), troopers[indexOfSniper].getY(), false)) {
                    return true;
                }
            } else if (indexOfSoldier != -1 && self.getDistanceTo(troopers[indexOfSoldier]) > 3) {
                if (targetTrooper!= null && canSeeOrCanShoot(self, targetTrooper, false)) {
                    if (troopers[indexOfSoldier].getHitpoints() <= 30) {
                        if (goOnPath(self, troopers[indexOfSoldier].getX(), troopers[indexOfSoldier].getY(), false)) {
                            return true;
                        }
                    } else {
                        if (canShootOnTarget(self, targetTrooper)) {
                            shootOnTarget(self, targetTrooper);
                            return true;
                        }
                    }
                }

                if (goOnPath(self, troopers[indexOfSoldier].getX(), troopers[indexOfSoldier].getY(), false)) {
                    return true;
                }
            } else if (indexOfCommander != -1 && self.getDistanceTo(troopers[indexOfCommander]) > 3) {
                if (targetTrooper!= null && canSeeOrCanShoot(self, targetTrooper, false)) {
                    if (troopers[indexOfCommander].getHitpoints() <= 30) {
                        if (goOnPath(self, troopers[indexOfCommander].getX(), troopers[indexOfCommander].getY(), false)) {
                            return true;
                        }
                    } else {
                        if (canShootOnTarget(self, targetTrooper)) {
                            shootOnTarget(self, targetTrooper);
                            return true;
                        }
                    }
                }

                if (goOnPath(self, troopers[indexOfCommander].getX(), troopers[indexOfCommander].getY(), false)) {
                    return true;
                }
            }
        }



        //управление снайпером, чтобы не лез на передовую при живых командире и солдате //TODO как контролировать передвижение снайпера?
        if (self.getType() == TrooperType.SNIPER && listOfEnemys != null && listOfEnemys.size() > 0 && (indexOfCommander != -1 || indexOfScout != -1 || indexOfSoldier != -1)) {

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
        if (listOfEnemys.size() != 0) {

            //не может быть равен -1, так как listOfEnemys не пуст.
            choosenOne = chooseEnemyOnDistance(self, listOfEnemys);
            if(choosenOne != null) {
                targetTrooper = choosenOne;
            }

            if (detectEnemyByTeam == false) {
                detectEnemyByTeam = true;
            }

            boolean isVisibleForEnemys = false;
            for (Trooper trooper : listOfEnemys) {
                if (world.isVisible(trooper.getVisionRange(), trooper.getX(), trooper.getY(), trooper.getStance(), self.getX(), self.getY(), self.getStance())) {
                    isVisibleForEnemys = true;
                    break;
                }
            }

            //логика обработки врагов. Если врагов: 1 ...
            if (listOfEnemys.size() == 1) {
                if ((indexOfSoldier != -1 && troopers[indexOfSoldier].getDistanceTo(targetTrooper) <= troopers[indexOfSoldier].getShootingRange() && world.isVisible(troopers[indexOfSoldier].getVisionRange(), troopers[indexOfSoldier].getX(), troopers[indexOfSoldier].getY(), troopers[indexOfSoldier].getStance(), targetTrooper.getX(), targetTrooper.getY(), targetTrooper.getStance())) || (indexOfSniper != -1 && troopers[indexOfSniper].getDistanceTo(targetTrooper) <= troopers[indexOfSniper].getShootingRange() && world.isVisible(troopers[indexOfSniper].getVisionRange(), troopers[indexOfSniper].getX(), troopers[indexOfSniper].getY(), troopers[indexOfSniper].getStance(), targetTrooper.getX(), targetTrooper.getY(), targetTrooper.getStance()))) {
                    Trooper trooper = listOfEnemys.get(0);
                    if (teamSupportCount > 0 && trooper.getStance() != TrooperStance.PRONE && self.getHitpoints() > 65 && trooper.getType() == TrooperType.SOLDIER || teamSupportCount > 0 && trooper.getStance() != TrooperStance.PRONE && trooper.getType() != TrooperType.SOLDIER && self.getHitpoints() > 65 || trueMapOfPoints[self.getX()][self.getY()] != 2 && trooper.getHitpoints() <= 30 && trooper.getStance() == TrooperStance.PRONE || trueMapOfPoints[self.getX()][self.getY()] != 2 && self.getHitpoints() > 65 && trooper.getHitpoints() <= 75 && trooper.getStance() != TrooperStance.PRONE && Math.random() * 3 == 0) {
                        isVisibleForEnemys = false;
                        goToSafePlace = false;
                        safePoint = null;
                    }
                }
            }

            //... или 2 ...
            if (listOfEnemys.size() == 2) {
                for (Trooper trooper : listOfEnemys) {
                    if (teamSupportCount > 0 && trooper.getStance() != TrooperStance.PRONE && self.getHitpoints() > 75 && trooper.getType() == TrooperType.SOLDIER || teamSupportCount > 0 && trooper.getStance() != TrooperStance.PRONE && trooper.getType() != TrooperType.SOLDIER && self.getHitpoints() > 65 || trueMapOfPoints[self.getX()][self.getY()] != 2 && trooper.getHitpoints() <= 30 && trooper.getStance() == TrooperStance.PRONE || trueMapOfPoints[self.getX()][self.getY()] != 2 && self.getHitpoints() > 75 && trooper.getHitpoints() <= 75 && trooper.getStance() != TrooperStance.PRONE && Math.random() * 4 == 0) {
                        isVisibleForEnemys = false;
                        goToSafePlace = false;
                        safePoint = null;
                    }
                }
            }

            //... или >= 3.
            if (listOfEnemys.size() >= 3) {
                if (self.getHitpoints() > 75) {
                    if (teamSupportCount >= 2) {
                        isVisibleForEnemys = false;
                        goToSafePlace = false;
                        safePoint = null;
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

                                move.setAction(ActionType.LOWER_STANCE);
                                return true;
                            }
                        }
                    }

                    for (Trooper trooper : listOfEnemys) {
                        if (canSeeOrCanShoot(trooper, self, false)) {
                            if (conductTheWar(self)) {
                                return true;
                            }
                        }
                    }
                }
            }

            killAnyEnemyUnit(self);   //TODO попробовать, так как иногда бежит к таргету, когда есть в поле зрения враги, в идеале переделать чтобы вообще палил, не смотря на хп если достижимы юниты чужие

            //стреляем, бежим ко врагу, начинаем контактный бой
            if (targetTrooper != null && canShootOnTarget(self, targetTrooper)) {
                if (self.getActionPoints() >= getCostMoveWithStance(self)) {
                    if (testForFreePassage(self)) {
                        return true;
                    }
                }
                shootOnTarget(self, targetTrooper);
                return true;
            } else {
                // если снайперу не хватает AP на стрельбу и мишень в зоне досягаемости, то он пытается леч
                if (targetTrooper != null && self.getType() == TrooperType.SNIPER && self.getActionPoints() < 9 && canSeeOrCanShoot(self, targetTrooper, false)) {
                    if (makeValidLowerStance(self, false)) {
                        return true;
                    }
                } else {
                    if (targetTrooper != null && self.getType() == TrooperType.SNIPER && self.getActionPoints() < 9 && self.getActionPoints() >= getCostMoveWithStance(self) && canSeeOrCanShoot(self, targetTrooper, false)) {
                        if (goOnPath(self, targetTrooper.getX(), targetTrooper.getY(), true)) {
                            return true;
                        }
                    }
                }
            }

            /*if(targetTrooper != null && self.getType() == TrooperType.FIELD_MEDIC) {*//*
            LinkedList<thePoint> pathTemp = lee(self, self.getX(), self.getY(), targetTrooper.getX(), targetTrooper.getY(), false);
            if (targetTrooper != null && !(canSeeOrCanShoot(self, targetTrooper, false) || canSeeOrCanShoot(self, targetTrooper, true)) && pathTemp != null && pathTemp.size() != 0 && pathTemp.size() != 1 && pathTemp.size() - 1 > self.getDistanceTo(targetTrooper) + 2) {
                    for (int i : listOfEnemys) {
                        Trooper trooper = troopers[i];
                        if (trooper.getId() != targetTrooper.getId() && self.getDistanceTo(trooper) <= self.getVisionRange() && canSeeOrCanShoot(trooper, self, true)) {
                            if (shootAndGoToSafePlace(self, trooper, true)) {
                                return true;
                            }
                        }
                    }
            }
                *//*}
            }*/

            if (targetTrooper != null && self.getActionPoints() >= getCostMoveWithStance(self) && !world.isVisible(self.getShootingRange(), self.getX(), self.getY(), self.getStance(), targetTrooper.getX(), targetTrooper.getY(), targetTrooper.getStance())) {
                if (goOnPath(self, targetTrooper.getX(), targetTrooper.getY(), false)) {
                    return true;
                }
            }

            if(self.getType() == TrooperType.FIELD_MEDIC) {
                boolean canMove = true;
                for (Trooper trooper : listOfEnemys) {
                    if (canSeeOrCanShoot(trooper, self, true)) {
                        canMove = false;
                        if (makeValidLowerStance(self, false)) { //TODO можно убрать
                            return true;
                        }
                    }
                }
                if (targetTrooper != null && self.getActionPoints() >= getCostMoveWithStance(self) && canMove) {
                    if (goOnPath(self, targetTrooper.getX(), targetTrooper.getY(), false)) {
                        return true;
                    }
                }
            }

            /*if (targetTrooper != null && self.getActionPoints() >= getCostMoveWithStance(self) * 4) {
                if (goOnPath(self, targetTrooper.getX(), targetTrooper.getY(), false)) {
                    return true;
                }
            }

            if (clearSelfArea(self, 1)) {
                if (targetTrooper != null && self.getActionPoints() >= getCostMoveWithStance(self) * 3) {
                    if (goOnPath(self, targetTrooper.getX(), targetTrooper.getY(), false)) {
                        return true;
                    }
                }
            }*/

            if (targetTrooper != null && isBetweenWalls(self.getX(), self.getY()) && self.getActionPoints() >= 6) {
                if (goOnPath(self, targetTrooper.getX(), targetTrooper.getY(), false)) {
                    return true;
                }
            }

            /*if (targetTrooper != null && self.getActionPoints() >= getCostMoveWithStance(self) && !canSeeOrCanShoot(self, targetTrooper, false)) {
                if (goOnPath(self, targetTrooper.getX(), targetTrooper.getY(), false)) {
                    return true;
                }
            }*/

        } else {
            //обработка бонусов, их подбор если нет в наличии

            if (safePoint != null) {
                LinkedList<thePoint> safePath = lee(self, self.getX(), self.getY(), safePoint.getX(), safePoint.getY(), true);
                if (safePath != null && safePath.size() != 0 && safePath.size() != 1 && self.getActionPoints() >= (safePath.size() - 1) * getCostMoveWithStance(self)) {
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

                    move.setAction(ActionType.SHOOT);
                    move.setX(targetUnit.trooper.getX());
                    move.setY(targetUnit.trooper.getY());
                    return true;
                }
            }

            if (bonuses != null) {

                for (Bonus bonus : bonuses) {
                    boolean isGoToBonus = true;

                    for (Trooper troop : troopers) {
                        if (troop.getX() == bonus.getX() && troop.getY() == bonus.getY()) {
                            isGoToBonus = false;
                        }
                    }

                    LinkedList<thePoint> pathTemp = lee(self, self.getX(), self.getY(), bonus.getX(), bonus.getY(), true);
                    if (pathTemp != null && pathTemp.size() > 4) {
                        isGoToBonus = false;
                    }

                    if (isGoToBonus && self.getDistanceTo(bonus) <= 3 && !self.isHoldingGrenade() && bonus.getType() == BonusType.GRENADE) {
                        goToBonus = true;
                        if (goOnPath(self, bonus.getX(), bonus.getY(), true)) {
                            return true;
                        }
                    }
                    if (isGoToBonus && self.getDistanceTo(bonus) <= 3 && !self.isHoldingMedikit() && bonus.getType() == BonusType.MEDIKIT) {
                        goToBonus = true;
                        if (goOnPath(self, bonus.getX(), bonus.getY(), true)) {
                            return true;
                        }
                    }
                    if (isGoToBonus && self.getDistanceTo(bonus) <= 3 && !self.isHoldingFieldRation() && bonus.getType() == BonusType.FIELD_RATION) {
                        goToBonus = true;
                        if (goOnPath(self, bonus.getX(), bonus.getY(), true)) {
                            return true;
                        }
                    }
                }
            }

            /*if (targetTrooper == null && self.getActionPoints() >= getCostMoveWithStance(self) * 3 && self.getType() == TrooperType.SNIPER && !goToSafePlace && targetHeal == null && (targetX == localTargetX && targetY == localTargetY || targetX == globalTargetX && targetY == globalTargetY)) {
                if(targetX == localTargetX && targetY == localTargetY || targetX == globalTargetX && targetY == globalTargetY) {
                    if (indexOfScout != -1 && self.getDistanceTo(targetX, targetY) <= troopers[indexOfScout].getDistanceTo(targetX, targetY) *//*&& world.isVisible(self.getShootingRange(), self.getX(), self.getY(), self.getStance(), targetX, targetY, TrooperStance.STANDING)*//*) {
                        if (!isUseLastMove && goOnPath(self, lastMoveX, lastMoveY, true)) {
                            isUseLastMove = true;
                            return true;
                        } else {
                            isUseLastMove = false;
                            move.setAction(ActionType.END_TURN);
                            return true;
                        }
                    } else if (indexOfCommander != -1 && self.getDistanceTo(targetX, targetY) <= troopers[indexOfCommander].getDistanceTo(targetX, targetY) *//*&& world.isVisible(self.getShootingRange(), self.getX(), self.getY(), self.getStance(), targetX, targetY, TrooperStance.STANDING)*//*) {
                        if (!isUseLastMove && goOnPath(self, lastMoveX, lastMoveY, true)) {
                            isUseLastMove = true;
                            return true;
                        } else {
                            isUseLastMove = false;
                            move.setAction(ActionType.END_TURN);
                            return true;
                        }
                    } else if (indexOfSoldier != -1 && self.getDistanceTo(targetX, targetY) <= troopers[indexOfSoldier].getDistanceTo(targetX, targetY) *//*&& world.isVisible(self.getShootingRange(), self.getX(), self.getY(), self.getStance(), targetX, targetY, TrooperStance.STANDING)*//*) {
                        if (!isUseLastMove && goOnPath(self, lastMoveX, lastMoveY, true)) {
                            isUseLastMove = true;
                            return true;
                        } else {
                            isUseLastMove = false;
                            move.setAction(ActionType.END_TURN);
                            return true;
                        }
                    } else if (indexOfMedic != -1 && self.getDistanceTo(targetX, targetY) <= troopers[indexOfMedic].getDistanceTo(targetX, targetY) *//*&& world.isVisible(self.getShootingRange(), self.getX(), self.getY(), self.getStance(), targetX, targetY, TrooperStance.STANDING)*//*) {
                        if (!isUseLastMove && goOnPath(self, lastMoveX, lastMoveY, true)) {
                            isUseLastMove = true;
                            return true;
                        } else {
                            isUseLastMove = false;
                            move.setAction(ActionType.END_TURN);
                            return true;
                        }
                    }
                }
            }*/

            if (targetTrooper == null && self.getActionPoints() >= getCostMoveWithStance(self) * 3 && self.getType() == TrooperType.SNIPER && !goToSafePlace && targetHeal == null && (targetX == localTargetX && targetY == localTargetY || targetX == globalTargetX && targetY == globalTargetY)) {
                if(targetX == localTargetX && targetY == localTargetY || targetX == globalTargetX && targetY == globalTargetY) {

                    LinkedList<thePoint> path1 = lee(self, self.getX(), self.getY(), targetX, targetY, false);
                    LinkedList<thePoint> path2 = null;

                    Trooper tempTrooper;
                    if (indexOfScout != -1) {
                        tempTrooper = troopers[indexOfScout];
                        path2 = lee(tempTrooper, tempTrooper.getX(), tempTrooper.getY(), targetX, targetY, false);

                        if (path1 != null && path2 != null && path2.size() - path1.size() > 0) {
                            if (!isUseLastMove && goOnPath(self, lastMoveX, lastMoveY, true)) {
                                isUseLastMove = true;
                                return true;
                            } else {
                                isUseLastMove = false;
                                move.setAction(ActionType.END_TURN);
                                return true;
                            }
                        }
                    } else {

                        if (indexOfCommander != -1) {

                            tempTrooper = troopers[indexOfCommander];
                            path2 = lee(tempTrooper, tempTrooper.getX(), tempTrooper.getY(), targetX, targetY, false);

                            if (path1 != null && path2 != null && path2.size() - path1.size() > 0) {
                                if (!isUseLastMove && goOnPath(self, lastMoveX, lastMoveY, true)) {
                                    isUseLastMove = true;
                                    return true;
                                } else {
                                    isUseLastMove = false;
                                    move.setAction(ActionType.END_TURN);
                                    return true;
                                }
                            }

                        } else {
                            if (indexOfSoldier != -1) {

                                tempTrooper = troopers[indexOfSoldier];
                                path2 = lee(tempTrooper, tempTrooper.getX(), tempTrooper.getY(), targetX, targetY, false);

                                if (path1 != null && path2 != null && path2.size() - path1.size() > 0) {
                                    if (!isUseLastMove && goOnPath(self, lastMoveX, lastMoveY, true)) {
                                        isUseLastMove = true;
                                        return true;
                                    } else {
                                        isUseLastMove = false;
                                        move.setAction(ActionType.END_TURN);
                                        return true;
                                    }
                                }
                            } else if (indexOfMedic != -1) {
                                tempTrooper = troopers[indexOfMedic];
                                path2 = lee(tempTrooper, tempTrooper.getX(), tempTrooper.getY(), targetX, targetY, false);

                                if (path1 != null && path2 != null && path2.size() - path1.size() > 0) {
                                    if (!isUseLastMove && goOnPath(self, lastMoveX, lastMoveY, true)) {
                                        isUseLastMove = true;
                                        return true;
                                    } else {
                                        isUseLastMove = false;
                                        move.setAction(ActionType.END_TURN);
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (targetTrooper == null &&  self.getActionPoints() >= getCostMoveWithStance(self) * 3 && self.getType() == TrooperType.FIELD_MEDIC && !goToSafePlace && targetHeal == null && (targetX == localTargetX && targetY == localTargetY || targetX == globalTargetX && targetY == globalTargetY)) {
                if(targetX == localTargetX && targetY == localTargetY || targetX == globalTargetX && targetY == globalTargetY) {

                    LinkedList<thePoint> path1 = lee(self, self.getX(), self.getY(), targetX, targetY, false);
                    LinkedList<thePoint> path2 = null;

                    Trooper tempTrooper;
                    if (indexOfScout != -1) {
                        tempTrooper = troopers[indexOfScout];
                        path2 = lee(tempTrooper, tempTrooper.getX(), tempTrooper.getY(), targetX, targetY, false);

                        if (path1 != null && path2 != null && path2.size() - path1.size() > 0) {
                            if (!isUseLastMove && goOnPath(self, lastMoveX, lastMoveY, true)) {
                                isUseLastMove = true;
                                return true;
                            } else {
                                isUseLastMove = false;
                                move.setAction(ActionType.END_TURN);
                                return true;
                            }
                        }
                    } else {

                        if (indexOfCommander != -1) {

                            tempTrooper = troopers[indexOfCommander];
                            path2 = lee(tempTrooper, tempTrooper.getX(), tempTrooper.getY(), targetX, targetY, false);

                            if (path1 != null && path2 != null && path2.size() - path1.size() > 0) {
                                if (!isUseLastMove && goOnPath(self, lastMoveX, lastMoveY, true)) {
                                    isUseLastMove = true;
                                    return true;
                                } else {
                                    isUseLastMove = false;
                                    move.setAction(ActionType.END_TURN);
                                    return true;
                                }
                            }
                        } else if (indexOfSoldier != -1) {
                            tempTrooper = troopers[indexOfSoldier];
                            path2 = lee(tempTrooper, tempTrooper.getX(), tempTrooper.getY(), targetX, targetY, false);

                            if (path1 != null && path2 != null && path2.size() - path1.size() > 0) {
                                if (!isUseLastMove && goOnPath(self, lastMoveX, lastMoveY, true)) {
                                    isUseLastMove = true;
                                    return true;
                                } else {
                                    isUseLastMove = false;
                                    move.setAction(ActionType.END_TURN);
                                    return true;
                                }
                            }
                        }
                    }
                }
            }


            if (self.getActionPoints() >= getCostMoveWithStance(self) * 4) {
                if (goOnPath(self, targetX, targetY, false)) {
                    return true;
                }
            }

            if (self.getType() == TrooperType.SNIPER && self.getActionPoints() >= 6 && self.getStance() != TrooperStance.STANDING/* && targetX == localTargetX && targetY == localTargetY*/) {
                if (goOnPath(self, targetX, targetY, false)) {
                    return true;
                }
            }

            if (clearSelfArea(self, 1)) {
                if (self.getActionPoints() >= getCostMoveWithStance(self) * 3) {
                    if (goOnPath(self, targetX, targetY, false)) {
                        return true;
                    }
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
                if (goOnPath(self, targetX, targetY, true)) {
                    return true;
                }
            }

            /*if (trueMapOfPoints[self.getX()][self.getY()] == 3 && self.getActionPoints() >= getCostMoveWithStance(self)) {
                if (goOnPath(self, localTargetX == 100 ? globalTargetX : localTargetX, localTargetY == 100 ? globalTargetY : localTargetY, true)) {
                    return true;
                }
            }*/

            //TODO сделать проверку на видимость этой клетки любым юнитом, возможно нужно оставить PRONE, так же в world.
            if (localTargetX != 100 && self.getType() == TrooperType.SNIPER && self.getActionPoints() >= getCostMoveWithStance(self) && !world.isVisible(/*AREA_OF_SNIPER self.getVisionRange()*/self.getShootingRange(), self.getX(), self.getY(), self.getStance(), localTargetX, localTargetY, TrooperStance.STANDING)) {
                if (goOnPath(self, localTargetX, localTargetY, false)) {
                    return true;
                }
            }
        }

        return false;
    }


    boolean conductTheWar(Trooper self) {
        beginBattle = true;
        if (testForFreePassage(self)) {
            return true;
        }

        if (saveOursSouls(self)) {
            return true;
        }

        if (targetTrooper != null && teamSupportCount < 2 && self.getDistanceTo(targetTrooper) > 5 && self.getActionPoints() >= 6 && self.getStance() == TrooperStance.STANDING) {
            goOnWar(self, globalTargetX, globalTargetY);
            return true;
        }

        if (listOfEnemys.size() != 0) {
            if (self.getActionPoints() >= self.getShootCost()) {

                Trooper choosenOne = chooseEnemyOnDistance(self, listOfEnemys);

                if (choosenOne != null) {
                    targetTrooper = choosenOne;

                    //пытаемся опуститься так, чтобы противник нас не видел, а мы его видели //TODO тоже какая то фигня бесполезная, проверить!!
                    if (self.getActionPoints() >= self.getShootCost() + game.getStanceChangeCost()) {
                        for (Trooper trooper : listOfEnemys) {
                            if (self.getStance() == TrooperStance.STANDING && world.isVisible(self.getShootingRange(), self.getX(), self.getY(), TrooperStance.KNEELING, trooper.getX(), trooper.getY(), trooper.getStance()) && !world.isVisible(trooper.getVisionRange(), trooper.getX(), trooper.getY(), trooper.getStance(), self.getX(), self.getY(), TrooperStance.KNEELING) && world.isVisible(trooper.getShootingRange() <= trooper.getVisionRange() ? trooper.getVisionRange() : trooper.getShootingRange(), trooper.getX(), trooper.getY(), trooper.getStance(), self.getX(), self.getY(), TrooperStance.STANDING)) {
                                if (self.getActionPoints() >= game.getStanceChangeCost()) {
                                    move.setAction(ActionType.LOWER_STANCE);
                                    return true;
                                }
                            } else if (self.getStance() == TrooperStance.KNEELING && world.isVisible(self.getShootingRange(), self.getX(), self.getY(), TrooperStance.PRONE, trooper.getX(), trooper.getY(), trooper.getStance()) && !world.isVisible(trooper.getVisionRange(), trooper.getX(), trooper.getY(), trooper.getStance(), self.getX(), self.getY(), TrooperStance.PRONE) && world.isVisible(trooper.getShootingRange() <= trooper.getVisionRange() ? trooper.getVisionRange() : trooper.getShootingRange(), trooper.getX(), trooper.getY(), trooper.getStance(), self.getX(), self.getY(), TrooperStance.KNEELING)) {
                                if (self.getActionPoints() >= game.getStanceChangeCost()) {
                                    move.setAction(ActionType.LOWER_STANCE);
                                    return true;
                                }
                            }
                        }
                    }

                    if (canSeeOrCanShoot(self, targetTrooper, false)) {

                        if (self.getActionPoints() >= self.getShootCost() + game.getStanceChangeCost() && makeValidLowerStance(self, true)) {
                            return true;
                        }

                        if (canShootOnTarget(self, targetTrooper)) {
                            shootOnTarget(self, targetTrooper);
                            return true;
                        }
                    } else {
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
            if (goOnWar(self, localTargetX, localTargetY)) {
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
//                                move.setAction(ActionType.END_TURN);
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

//                          проверка клетки куда собираемся ходить на занятость своими или чужими юнитами
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
//                                move.setAction(ActionType.END_TURN);
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


    thePoint findNotAchievableTail(Trooper self, boolean seeOrShoot) {
        //находит ячейку недосягаемую для чужих юнитов
        //TODO учесть стойку, она влияет на видимость, возможно достаточно будет просто присесть
        int W = world.getWidth();
        int H = world.getHeight();
        int WALL = -1;                // непроходимая ячейка
        int BLANK = -2;                // свободная непомеченная ячейка

        int[][] cellsIntTemp = new int[W][];
        for (int i = 0; i < W; i++) {
            cellsIntTemp[i] = Arrays.copyOf(cellsInt[i], cellsInt[i].length);
        }

        if (seeOrShoot) {
            for (Trooper trooper : listOfEnemys) {
                for (int k = 0; k < W; k++) {
                    for (int m = 0; m < H; m++) {
                        if (trooper.getDistanceTo(k, m) <= trooper.getVisionRange() && cellsIntTemp[k][m] != WALL && world.isVisible(trooper.getVisionRange(), trooper.getX(), trooper.getY(), trooper.getStance(), k, m, self.getStance())) {
                            cellsIntTemp[k][m] = WALL;
                        }
                    }
                }
            }
        } else {
            for (Trooper trooper : listOfEnemys) {
                for (int k = 0; k < W; k++) {
                    for (int m = 0; m < H; m++) {
                        if (trooper.getDistanceTo(k, m) <= trooper.getShootingRange() && cellsIntTemp[k][m] != WALL && world.isVisible(trooper.getShootingRange(), trooper.getX(), trooper.getY(), trooper.getStance(), k, m, self.getStance())) {
                            // в условеии было trooper.getDistanceTo(k, m) <= trooper.getVisionRange())
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

        for (int k = 0; k < W; k++) {
            for (int m = 0; m < H; m++) {
                if (cellsIntTemp[k][m] == BLANK && self.getDistanceTo(k, m) <= moveLen) {
                    LinkedList<thePoint> tempPath = lee(self, self.getX(), self.getY(), k, m, true);
                    for (Trooper trooper : troopers) {
                        if (trooper.getX() == k && trooper.getY() == m) {
                            tempPath = null;
                            break;
                        }
                    }
                    if (tempPath != null && tempPath.size() != 0 && tempPath.size() != 1 && tempPath.size() - 1 < moveLen) {
                        moveLen = tempPath.size() - 1;
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
        } else if (commanderNotDead == 1) {
            return commanderNotDeadTrooper;
        } else if (soldierNotDead == 1) {
            return soldierNotDeadTrooper;
        } else if (sniperNotDead == 1) {
            return sniperNotDeadTrooper;
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
                }

                if (Math.abs(hp1 - hp2) <= 30 && Math.abs(dist1 - dist2) <= 2 && listOfEnemy.size() >= 2) {
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

        for (Trooper trooper : listOfEnemy) {
            if (self.getDistanceTo(trooper) < minDistance /*&& world.isVisible(self.getShootingRange(), self.getX(), self.getY(), self.getStance(), trooper.getX(), trooper.getY(), trooper.getStance())*/) {
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

        // Перед вызовом lee() массив grid заполнен значениями WALL и BLANK

        /*if (world.getMoveIndex() == 0 && cellsInt == null) {

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
        }*/

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

    boolean isDistanceEqualOrLessOneTail(Trooper self, Trooper target) {
        return self.getDistanceTo(target) <= 1;
    }


//    //    flag = 1 | 0; 1 - для расчёта стрельбы (shoot), 0 - для расчёта видимости цели (vision).
//    Trooper getGoal(World world, Trooper self, Trooper[] troopers, boolean isLowHPNotIsClosest) {
//        double distanceToEnemy = 15;
//        int indexOfShortestEnemy = -1;
//        int indexOfLowHPEnemy = -1;
//        int minHP = 120;
//
//        if(isLowHPNotIsClosest){
//            for (int i = 0; i < troopers.length; ++i) {
//                if(!troopers[i].isTeammate()) {
//                    if(troopers[i].getHitpoints() <= minHP){
//                        indexOfLowHPEnemy = i;
//                        minHP = troopers[i].getHitpoints();
//                    }
//                }
//            }
//
//            if(indexOfLowHPEnemy == -1) {
//                return null;
//            }
//
//            return troopers[indexOfLowHPEnemy];
//
//        } else {
//            for (int i = 0; i < troopers.length; ++i) {
//                if(!troopers[i].isTeammate()) {
//                    if(self.getDistanceTo(troopers[i]) < distanceToEnemy){
//                        indexOfShortestEnemy = i;
//                        distanceToEnemy = self.getDistanceTo(troopers[i]);
//                    }
//                }
//            }
//
//            if(indexOfShortestEnemy == -1) {
//                return null;
//            }
//            return troopers[indexOfShortestEnemy];
//        }
//
//    }

    //TODO сделать так, чтобы медик наконец перестал хиляться когда по нему стреляют, а отбегал хотя б на шаг или пару шагов!
    boolean goHeal(Trooper self) {
        boolean needHeal;
        boolean needHealTarget = false;

        /*if (detectEnemyByTeam && beginBattle == false) {
            if (self.getType() == TrooperType.FIELD_MEDIC) {
                if (goDown(self)) {
                    return true;
                }
            }
        }*/

        if (targetTrooper != null && self.getHitpoints() < 65) {
            boolean safePlace = true;
            for (Trooper trooper : listOfEnemys) {
                if (canSeeOrCanShoot(trooper, self, false) || canSeeOrCanShoot(trooper, self, true)) {
                    safePlace = false;
                    break;
                }
            }
            if (safePlace) {
                targetHeal = self;
            } else {
                thePoint tempPoint1 = findNotAchievableTail(self, true);
                thePoint tempPoint2 = findNotAchievableTail(self, false);
                thePoint tempPoint;

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
                    if (tempPath1 != null && tempPath1.size() != 0 && tempPath1.size() != 1 && tempPath1.size() >= /*tempPath2.size() + 5*/self.getDistanceTo(tempPoint.getX(), tempPoint.getY()) + 7 && self.getActionPoints() >= (tempPath1.size() - 1) * getCostMoveWithStance(self)) {
                        if (goOnPath(self, tempPoint.getX(), tempPoint.getY(), true)) {
                            return true;
                        }
                    } else if (tempPath2 != null && tempPath2.size() != 0 && tempPath2.size() != 1 && self.getActionPoints() >= (tempPath2.size() - 1) * getCostMoveWithStance(self)) {
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
                            for (Trooper trooper : listOfEnemys) {
                                if (canSeeOrCanShoot(trooper, self, false) || canSeeOrCanShoot(trooper, self, true)) {
                                    safePlace = false;
                                    break;
                                }
                            }
                            if (safePlace) {
                                targetHeal = self;
                            } else {
                                thePoint tempPoint1 = findNotAchievableTail(self, true);
                                thePoint tempPoint2 = findNotAchievableTail(self, false);
                                thePoint tempPoint;

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
                                    if (tempPath1 != null && tempPath1.size() != 0 && tempPath1.size() != 1 && tempPath1.size() >= /*tempPath2.size() + 5*/self.getDistanceTo(tempPoint.getX(), tempPoint.getY()) + 7 && self.getActionPoints() >= (tempPath1.size() - 1) * getCostMoveWithStance(self)) {
                                        if (goOnPath(self, tempPoint.getX(), tempPoint.getY(), true)) {
                                            return true;
                                        }
                                    } else if (tempPath2 != null && tempPath2.size() != 0 && tempPath2.size() != 1 && self.getActionPoints() >= (tempPath2.size() - 1) * getCostMoveWithStance(self)) {
                                        if (goOnPath(self, tempPoint.getX(), tempPoint.getY(), false)) {
                                            return true;
                                        }
                                    }
                                }
                            }
                        }
                        heal(self, targetHeal);
                        return true;
                    } else {
                        boolean canMove = true;
                        for (Trooper trooper : troopers) {
                            LinkedList<thePoint> tempPath = lee(self, self.getX(), self.getY(), targetHeal.getX(), targetHeal.getY(), true);
                            if (!trooper.isTeammate() && self.getDistanceTo(trooper) <= self.getVisionRange() && tempPath != null && tempPath.size() != 0 && tempPath.size() != 1 && tempPath.size() - 1 >= self.getDistanceTo(targetHeal) + 7) {
                                canMove = false;
                            }
                        }
                        if (targetHeal != null && self.getActionPoints() >= getCostMoveWithStance(self) && !(self.getDistanceTo(targetHeal) <= 1) && canMove) {
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
                } else {
                    for (Trooper trooper : troopers) {
                        needHeal = trooper.isTeammate() && trooper.getHitpoints() < HP_WHEN_HEAL;
                        if (needHeal) {
                            if (targetHeal == null) {
                                targetHeal = trooper;
                            }

                            Trooper trooper1 = null;
                            if (self.getDistanceTo(trooper) <= 1) {
                                if (targetHeal.getId() == self.getId() && targetTrooper != null && self.getHitpoints() < 76) {
                                    boolean safePlace = true;
                                    for (Trooper trooper2 : listOfEnemys) {
                                        if (canSeeOrCanShoot(trooper2, self, false) || canSeeOrCanShoot(trooper2, self, true)) {
                                            safePlace = false;
                                            break;
                                        }
                                    }
                                    if (safePlace) {
                                        targetHeal = self;
                                    } else {
                                        thePoint tempPoint1 = findNotAchievableTail(self, true);
                                        thePoint tempPoint2 = findNotAchievableTail(self, false);
                                        thePoint tempPoint;

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
                                            if (tempPath1 != null && tempPath1.size() != 0 && tempPath1.size() != 1 && tempPath1.size() >= /*tempPath2.size() + 5*/self.getDistanceTo(tempPoint.getX(), tempPoint.getY()) + 7 && self.getActionPoints() >= (tempPath1.size() - 1) * getCostMoveWithStance(self)) {
                                                if (goOnPath(self, tempPoint.getX(), tempPoint.getY(), true)) {
                                                    return true;
                                                }
                                            } else if (tempPath2 != null && tempPath2.size() != 0 && tempPath2.size() != 1 && self.getActionPoints() >= (tempPath2.size() - 1) * getCostMoveWithStance(self)) {
                                                if (goOnPath(self, tempPoint.getX(), tempPoint.getY(), false)) {
                                                    return true;
                                                }
                                            }
                                        }
                                    }
                                }
                                heal(self, trooper);
                                return true;
                            } else {
                                boolean canMove = true;
                                for (Trooper troop : troopers) {
                                    LinkedList<thePoint> tempPath = lee(self, self.getX(), self.getY(), troop.getX(), troop.getY(), true);
                                    if (!troop.isTeammate() && self.getDistanceTo(troop) <= self.getVisionRange() && tempPath != null && tempPath.size() != 0 && tempPath.size() != 1 && tempPath.size() - 1 >= self.getDistanceTo(troop) + 5) {
                                        canMove = false;
                                        trooper1 = troop;
                                        break;
                                    }
                                }
                                if (self.getActionPoints() >= getCostMoveWithStance(self) && !isDistanceEqualOrLessOneTail(self, trooper) && canMove) {
                                    LinkedList<thePoint> path1 = lee(self, self.getX(), self.getY(), targetHeal.getX(), targetHeal.getY(), true);
                                    LinkedList<thePoint> path2 = lee(self, self.getX(), self.getY(), targetHeal.getX(), targetHeal.getY(), false);

                                    if (path1 != null && path1.size() <= path2.size() + 5) {
                                        if (goOnPath(self, trooper.getX(), trooper.getY(), true)) {
                                            return true;
                                        }
                                    }/* else if (trooper1 != null) { //TODO посмотреть как можно указать, чтобы медик атачил ближнего врага, возможно в goonwar
                                        goOnWar(self, trooper1.getX(), trooper1.getY());
                                        return true;
                                    }*/ else if (goOnPath(self, trooper.getX(), trooper.getY(), false)) {
                                        return true;
                                    }
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
//            if (self.getDistanceTo(targetX, targetY) <= AREA_OF_GRENADE) {
            return new thePoint(targetX, targetY);
//            }
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
        if (listOfEnemys.size() > 1) {
            thePoint targetPointForGrenade;
            int raznicaX;
            int raznicaY;

            for (int i = 0; i <= listOfEnemys.size() - 2; i++) {
                for (int j = i + 1; j <= listOfEnemys.size() - 1; j++) {

                    if (Math.abs(listOfEnemys.get(i).getX() - listOfEnemys.get(j).getX()) <= 2 && Math.abs(listOfEnemys.get(i).getY() - listOfEnemys.get(j).getY()) <= 2 && listOfEnemys.get(i).getDistanceTo(listOfEnemys.get(j)) <= 2) {

                        raznicaX = listOfEnemys.get(i).getX() - listOfEnemys.get(j).getX();

                        switch (raznicaX) {
                            case -2: {
                                targetPointForGrenade = tryToUseGrenade(self, listOfEnemys.get(i).getX() + 1, listOfEnemys.get(i).getY());
                                if (targetPointForGrenade != null) {
                                    listOfGrenadePoint.add(targetPointForGrenade);
                                }
                                break;
                            }
                            case -1: {
                                raznicaY = listOfEnemys.get(i).getY() - listOfEnemys.get(j).getY();
                                switch (raznicaY) {
                                    case 1: {
                                        targetPointForGrenade = tryToUseGrenade(self, listOfEnemys.get(i).getX() + 1, listOfEnemys.get(i).getY());
                                        if (targetPointForGrenade != null) {
                                            listOfGrenadePoint.add(targetPointForGrenade);
                                        }
                                        targetPointForGrenade = tryToUseGrenade(self, listOfEnemys.get(i).getX(), listOfEnemys.get(i).getY() - 1);
                                        if (targetPointForGrenade != null) {
                                            listOfGrenadePoint.add(targetPointForGrenade);
                                        }
                                        break;
                                    }
                                    case 0: {
                                        targetPointForGrenade = tryToUseGrenade(self, listOfEnemys.get(i).getX(), listOfEnemys.get(i).getY());
                                        if (targetPointForGrenade != null) {
                                            listOfGrenadePoint.add(targetPointForGrenade);
                                        }
                                        targetPointForGrenade = tryToUseGrenade(self, listOfEnemys.get(j).getX(), listOfEnemys.get(j).getY());
                                        if (targetPointForGrenade != null) {
                                            listOfGrenadePoint.add(targetPointForGrenade);
                                        }
                                        break;
                                    }
                                    case -1: {
                                        targetPointForGrenade = tryToUseGrenade(self, listOfEnemys.get(i).getX() + 1, listOfEnemys.get(i).getY());
                                        if (targetPointForGrenade != null) {
                                            listOfGrenadePoint.add(targetPointForGrenade);
                                        }
                                        targetPointForGrenade = tryToUseGrenade(self, listOfEnemys.get(i).getX(), listOfEnemys.get(i).getY() + 1);
                                        if (targetPointForGrenade != null) {
                                            listOfGrenadePoint.add(targetPointForGrenade);
                                        }
                                        break;
                                    }
                                }
                                break;
                            }
                            case 0: {
                                raznicaY = listOfEnemys.get(i).getY() - listOfEnemys.get(j).getY();
                                switch (raznicaY) {
                                    case 2: {
                                        targetPointForGrenade = tryToUseGrenade(self, listOfEnemys.get(i).getX(), listOfEnemys.get(i).getY() - 1);
                                        if (targetPointForGrenade != null) {
                                            listOfGrenadePoint.add(targetPointForGrenade);
                                        }
                                        break;
                                    }
                                    case 1: {
                                        targetPointForGrenade = tryToUseGrenade(self, listOfEnemys.get(i).getX(), listOfEnemys.get(i).getY());
                                        if (targetPointForGrenade != null) {
                                            listOfGrenadePoint.add(targetPointForGrenade);
                                        }
                                        targetPointForGrenade = tryToUseGrenade(self, listOfEnemys.get(j).getX(), listOfEnemys.get(j).getY());
                                        if (targetPointForGrenade != null) {
                                            listOfGrenadePoint.add(targetPointForGrenade);
                                        }
                                        break;
                                    }
                                    case -1: {
                                        targetPointForGrenade = tryToUseGrenade(self, listOfEnemys.get(i).getX(), listOfEnemys.get(i).getY());
                                        if (targetPointForGrenade != null) {
                                            listOfGrenadePoint.add(targetPointForGrenade);
                                        }
                                        targetPointForGrenade = tryToUseGrenade(self, listOfEnemys.get(j).getX(), listOfEnemys.get(j).getY());
                                        if (targetPointForGrenade != null) {
                                            listOfGrenadePoint.add(targetPointForGrenade);
                                        }
                                        break;
                                    }
                                    case -2: {
                                        targetPointForGrenade = tryToUseGrenade(self, listOfEnemys.get(i).getX(), listOfEnemys.get(i).getY() - 1);
                                        if (targetPointForGrenade != null) {
                                            listOfGrenadePoint.add(targetPointForGrenade);
                                        }
                                        break;
                                    }
                                }
                                break;
                            }
                            case 1: {
                                raznicaY = listOfEnemys.get(i).getY() - listOfEnemys.get(j).getY();
                                switch (raznicaY) {
                                    case 1: {
                                        targetPointForGrenade = tryToUseGrenade(self, listOfEnemys.get(i).getX() - 1, listOfEnemys.get(i).getY());
                                        if (targetPointForGrenade != null) {
                                            listOfGrenadePoint.add(targetPointForGrenade);
                                        }
                                        targetPointForGrenade = tryToUseGrenade(self, listOfEnemys.get(i).getX(), listOfEnemys.get(i).getY() - 1);
                                        if (targetPointForGrenade != null) {
                                            listOfGrenadePoint.add(targetPointForGrenade);
                                        }
                                        break;
                                    }
                                    case 0: {
                                        targetPointForGrenade = tryToUseGrenade(self, listOfEnemys.get(i).getX(), listOfEnemys.get(i).getY());
                                        if (targetPointForGrenade != null) {
                                            listOfGrenadePoint.add(targetPointForGrenade);
                                        }
                                        targetPointForGrenade = tryToUseGrenade(self, listOfEnemys.get(j).getX(), listOfEnemys.get(j).getY());
                                        if (targetPointForGrenade != null) {
                                            listOfGrenadePoint.add(targetPointForGrenade);
                                        }
                                        break;
                                    }
                                    case -1: {
                                        targetPointForGrenade = tryToUseGrenade(self, listOfEnemys.get(i).getX() - 1, listOfEnemys.get(i).getY());
                                        if (targetPointForGrenade != null) {
                                            listOfGrenadePoint.add(targetPointForGrenade);
                                        }
                                        targetPointForGrenade = tryToUseGrenade(self, listOfEnemys.get(i).getX(), listOfEnemys.get(i).getY() + 1);
                                        if (targetPointForGrenade != null) {
                                            listOfGrenadePoint.add(targetPointForGrenade);
                                        }
                                        break;
                                    }
                                }
                                break;
                            }
                            case 2: {
                                targetPointForGrenade = tryToUseGrenade(self, listOfEnemys.get(i).getX() - 1, listOfEnemys.get(i).getY());
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
            if (world.getMoveIndex() >= 7) {
                if (teamCount < 3) {
                    if (targetTrooper != null && self.getDistanceTo(targetTrooper.getX(), targetTrooper.getY()) <= AREA_OF_GRENADE) {
                        useGrenade(self, targetTrooper.getX(), targetTrooper.getY());
                        return true;
                    }
                }
            }

            /*if (listOfEnemys.size() == 1 || listOfEnemys.size() == 0) {
                targetListOfPointForGrenade = takeAPointsForGrenadeThrow(self, listOfSowEnemys);
            } else {
                targetListOfPointForGrenade ;
            }*/

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
                                for (Trooper trooper : listOfEnemys) {
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
                            LinkedList<thePoint> pathTemp1 = lee(self, self.getX(), self.getY(), targetForMovePoint.getX(), targetForMovePoint.getY(), true);
                            if (pathTemp1 != null && pathTemp1.size() != 0 && pathTemp1.size() != 1 && enemyCountNearPointOfGrenade >= maxEnemyNum && self.getActionPoints() - ACTION_POINT_OF_GRENADE_THROW + (self.isHoldingFieldRation() ? 3 : 0) >= (pathTemp1.size() - 1) * 2) {
                                if (targetPoint.getX() == self.getX() && targetPoint.getY() == self.getY() && enemyCountNearPointOfGrenade > maxEnemyNum) {
                                    targetPoint = point;
                                    maxEnemyNum = enemyCountNearPointOfGrenade;
                                    targetMovePoint = targetForMovePoint;
                                } else if (enemyCountNearPointOfGrenade >= maxEnemyNum && pathTemp1.size() - 1 < pathSize){
                                    targetPoint = point;
                                    maxEnemyNum = enemyCountNearPointOfGrenade;
                                    pathSize = pathTemp1.size() - 1;
                                    targetMovePoint = targetForMovePoint;
                                }
                            }
                        }
                    }
                }

                /*if (listOfEnemys.size() < 2 && killAnyEnemyUnit(self)) { //TODO возможно стоит убрать или увеличить до 2, потестить
                    return true;
                }*/

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

            if (tempPath != null && tempPath.size() != 0 && tempPath.size() != 1 && tempPath.size() - 1 < distance) {
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
                    if (tempPath != null && tempPath.size() != 0 && tempPath.size() != 1 && tempPath.size() - 1 < moveLen) {
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

                if (canShoot && self.getStance() != TrooperStance.PRONE && self.getDistanceTo(targetTrooper) <= self.getShootingRange() && ((self.getActionPoints() >= self.getShootCost() + game.getStanceChangeCost()) && inWar || (self.getActionPoints() >= game.getStanceChangeCost()) && !inWar)) {
                    move.setAction(ActionType.LOWER_STANCE);
                    return true;
                }
            } else {
                if (targetTrooper != null) {
                    for (Trooper trooper : listOfEnemys) {
                        boolean canShoot;
                        if (self.getStance() == TrooperStance.STANDING) {
                            canShoot = world.isVisible(self.getShootingRange(), self.getX(), self.getY(), TrooperStance.KNEELING, trooper.getX(), trooper.getY(), trooper.getStance());
                        } else if (self.getStance() == TrooperStance.KNEELING) {
                            canShoot = world.isVisible(self.getShootingRange(), self.getX(), self.getY(), TrooperStance.PRONE, trooper.getX(), trooper.getY(), trooper.getStance());
                        } else {
                            canShoot = false;
                        }

                        if (canShoot && self.getStance() != TrooperStance.PRONE && self.getDistanceTo(trooper) <= self.getShootingRange() && self.getActionPoints() >= /*self.getShootCost() +*/ game.getStanceChangeCost()) {
                            move.setAction(ActionType.LOWER_STANCE);
                            return true;
                        }
                    }
                } else {
                    if (localTargetX != 100) {
                        boolean canShoot;
                        if (self.getStance() == TrooperStance.STANDING) {
                            canShoot = world.isVisible(self.getShootingRange(), self.getX(), self.getY(), TrooperStance.KNEELING, localTargetX, localTargetY, TrooperStance.STANDING);
                        } else if (self.getStance() == TrooperStance.KNEELING) {
                            canShoot = world.isVisible(self.getShootingRange(), self.getX(), self.getY(), TrooperStance.PRONE, localTargetX, localTargetY, TrooperStance.STANDING);
                        } else {
                            canShoot = false;
                        }

                        if (canShoot && self.getStance() != TrooperStance.PRONE && self.getDistanceTo(localTargetX, localTargetY) <= self.getShootingRange() + 1 && self.getActionPoints() >= self.getShootCost() + game.getStanceChangeCost()) {
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
        if (self.getActionPoints() >= self.getShootCost() && self.getDistanceTo(target) <= self.getShootingRange()) {
            move.setAction(ActionType.SHOOT);
            move.setX(target.getX());
            move.setY(target.getY());
        }
    }


    void makeATarget(Trooper self) {

        if (getHelpFromAir && targetTrooper == null && localTargetX == 100) {

            //реагирование на crash стратегии для игры 4хХ
            isEnemyStrategyCrashed = false;
            Player[] playersTest = world.getPlayers();
            for (Player player : playersTest) {
                if (!player.getName().equalsIgnoreCase("darkstone") && player.isStrategyCrashed() && player.getApproximateX() != -1) {
                    thePoint point = testCellOnFree(player.getApproximateX(), player.getApproximateY());
                    globalTargetX = point.getX();
                    globalTargetY = point.getY();
                    isEnemyStrategyCrashed = true;
                    needHelpFromAir = false;
                    break;
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

            int xt = world.getWidth() / 2, yt = world.getHeight() / 2;
            /*if (self.getDistanceTo(0, 0) <= self.getDistanceTo(0, world.getHeight())) {
                yt = 3;
            } else {
                yt = world.getHeight() - 3;
            }*/


            thePoint pointTemp = testCellOnFree(xt, yt);
            if (pointTemp != null) {
                globalTargetX = pointTemp.getX();
                globalTargetY = pointTemp.getY();
            }

            for (int i = 0; i < 4; i++) {
                remainingQuarters.add(i);
            }

//            randomTarget(self.getX(), self.getY());
        }

        if (self.getDistanceTo(globalTargetX, globalTargetY) <= MIN_DISTANCE_FOR_LOCALTARGET) {
            if(indexOfCommander != -1) {
                needHelpFromAir = true;
            } else {
                randomTarget(self.getX(), self.getY());
            }
        }

//      Обнуление локальной цели при её достижимости

        if (self.getDistanceTo(localTargetX, localTargetY) <= MIN_DISTANCE_FOR_LOCALTARGET) {
            localTargetX = 100;
            localTargetY = 100;
            beginBattle = false;
        }

        boolean isTargetNeedChange = true;

        if (targetTrooper != null) {
            for (Trooper trooper : listOfEnemys) {
                if (trooper.getX() == targetTrooper.getX() && trooper.getY() == targetTrooper.getY()) {
                    isTargetNeedChange = false;
                    break;
                }
            }
            if (isTargetNeedChange) {
                localTargetX = targetTrooper.getX();
                localTargetY = targetTrooper.getY();
            }
        }

        if (isTargetNeedChange) {
            targetTrooper = null;
            if (listOfEnemys.size() != 0) {
                Trooper choosenOne = chooseEnemyOnDistance(self, listOfEnemys);
                if (choosenOne != null) {
                    targetTrooper = choosenOne;
                }
            }
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

    /*//TODO проверить
    boolean killTargetOnDistance (Trooper self) {
        if(!(self.getDistanceTo(targetTrooper) <= self.getShootingRange() && world.isVisible(self.getShootingRange(), self.getX(), self.getY(), self.getStance(), targetTrooper.getX(), targetTrooper.getY(), targetTrooper.getStance()))) {

            LinkedList<thePoint> tempList = lee(self.getX(), self.getY(), targetTrooper.getX(), targetTrooper.getY(), true);

            double leftScoreOfMove = self.getActionPoints() / 2 - (tempList.size() - 1) * getCostMoveWithStance(self) + self.getShootingRange();

            int moves = 0;
            if(tempList != null) {
                for (int i = 0; i < tempList.size(); ++i) {
                    if (self.getDistanceTo(targetTrooper) <= self.getShootingRange() && world.isVisible(self.getShootingRange(), tempList.get(i).getX(), tempList.get(i).getY(), self.getStance(), targetTrooper.getX(), targetTrooper.getY(), targetTrooper.getStance())) {
                        moves = i;
                        if (targetTrooper.getHitpoints() < (leftScoreOfMove - moves) * (self.getDamage(self.getStance())) ) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }*/


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

    boolean killAnyEnemyUnit(Trooper self) {
        //пытается убить любую вражескую цель, если она убиваема
        for (Trooper trooper : listOfEnemys) {
            if (trooper.getHitpoints() / self.getDamage(self.getStance()) + 1 <= self.getActionPoints() / self.getShootCost()) {
                if (canShootOnTarget(self, trooper)) {
                    shootOnTarget(self, trooper);
                    return true;
                }
            }
        }
        return false;
    }


    //TODO если хп юнита меняется, а врага не было видно, значит вряг рядом и нужно что-то делать, реализовать
    boolean hpIsChanged(Trooper self) {
        //TODO добавить вычисление направления и передвижения в эту сторону на 2-3 хода
        if (targetTrooper == null) {
            for (int i = 0; i < troopers.length; i++) {
                for (int j = 0; j < hpOfTroopers.length; j++) {
                    if (troopers[i].getId() == hpOfTroopers[j][0] && troopers[i].getHitpoints() < hpOfTroopers[j][1]) {
                        trooperUnderAttack = (int) troopers[i].getId();
                        return true;
                    }
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


    boolean underAttack(Trooper self) {
        if (istroopersUnderAttack && trooperUnderAttack == self.getId() && targetTrooper == null) {
            if (goOnWar(self, localTargetX != 100 ? localTargetX : globalTargetX, localTargetY != 100 ? localTargetY : globalTargetY)) {
                istroopersUnderAttack = false;
                return true;
            }
        }
        return false;
    }

    boolean goToMedic(Trooper self) {

        if (self.getHitpoints() < HP_WHEN_GO_MEDIC && self.getActionPoints() >= getCostMoveWithStance(self) && self.getType() != TrooperType.FIELD_MEDIC) {

            if (indexOfMedic != -1 && self.getDistanceTo(troopers[indexOfMedic]) > 1) {

                LinkedList<thePoint> path1 = lee(self, self.getX(), self.getY(), troopers[indexOfMedic].getX(), troopers[indexOfMedic].getY(), true);
                LinkedList<thePoint> path2 = lee(self, self.getX(), self.getY(), troopers[indexOfMedic].getX(), troopers[indexOfMedic].getY(), false);

                if (path1 != null && path1.size() <= path2.size() + 5) {
                    if (fireAndExit(self, path1)) {
                        return true;
                    }

                    if (goOnPath(self, troopers[indexOfMedic].getX(), troopers[indexOfMedic].getY(), true)) {
                        return true;
                    }
                } else if (path1 != null && path1.size() > path2.size() + 5) {
                    if (fireAndExit(self, path1)) {
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
            thePoint point = findNotAchievableTail(self, true);
            if (point != null) {

                LinkedList<thePoint> pathTempT = lee(self, self.getX(), self.getY(), point.getX(), point.getY(), true);
                LinkedList<thePoint> pathTempF = lee(self, self.getX(), self.getY(), point.getX(), point.getY(), false);

                int actionPoints = self.getActionPoints();
                int bonusFromCommander = game.getCommanderAuraBonusActionPoints();
                int goUpStance = 0;
                int afterUseRationFieldPoints = 3;

                if (self.getStance() == TrooperStance.PRONE) {
                    goUpStance = 4;
                } else if (self.getStance() == TrooperStance.KNEELING) {
                    goUpStance = 2;
                } else if (self.getStance() == TrooperStance.STANDING) {
                    goUpStance = 0;
                }

                /*if (indexOfCommander != -1 && self.getDistanceTo(troopers[indexOfCommander]) <= 5) {
                    actionPoints += bonusFromCommander;
                }*/

                if (self.isHoldingFieldRation()) {
                    actionPoints += afterUseRationFieldPoints;
                }

                actionPoints -= goUpStance;

                if (pathTempT != null && pathTempT.size() != 0 && pathTempT.size() != 1 && pathTempT.size() <= pathTempF.size() + 5) {
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


    /* //TODO расчитать середину не тронутых и середину тронутых юнитов, построить прямую через эти точки и пойти по
     //TODO прямой от середины нетронут. через середину тронут. на расстояние +2 или +3 от середины тронут.
    void goToTheAllegedEnemy(Trooper self) {
        int count = 0;
        for (Trooper trooper : troopers) {
            if (trooper.getType() != troopers[trooperUnderAttack].getType() && trooper.isTeammate()) {
                count++;
            }
        }
        switch (count) {
            case 4 : {

            }
            case 3 : {

            }
            case 2 : {
                LinkedList<Trooper> teamTroopers = new LinkedList<>();
                for (Trooper trooper : troopers) {
                    if (trooper.getType() != troopers[trooperUnderAttack].getType() && trooper.isTeammate()) {
                        teamTroopers.add(trooper);
                    }
                }

                thePoint point = ()
            }
            case 1 : {

            }
            case 0 : {

            }
        }
    }*/

    boolean clearSelfArea(Trooper self, int freeCellsOrFreeForVisionWhenStanding) {
        CellType[][] cells = world.getCells();
        int x1 = self.getX() - 1;
        int y1 = self.getY() - 1;
        int x2 = self.getX() + 1;
        int y2 = self.getY() + 1;

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
                switch (freeCellsOrFreeForVisionWhenStanding) {
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

                    /*case 2 : {
                        int enemyCountNearPointOfGrenade = 0;
                        for (int k : listOfEnemys) {
                            Trooper trooper = troopers[k];
                            if (self.getX() == i && self.getY() == j) {
                                enemyCountNearPointOfGrenade++;
                            }
                        }
                    }*/
                }
            }
        }

        return true;
    }


    boolean fireAndExit (Trooper self, LinkedList<thePoint> path) {

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

        //TODO проверить как с этим будет работать
        /*if (self.isHoldingFieldRation()) {
            actionPoints += afterUseRationFieldPoints;
        }*/

        if (path.size() != 0 && path.size() != 1 && path.size() != 2) {
            actionPoints -= goUpStance;
        }

        if ((path.size() - 1) * 2 > actionPoints) {

            boolean isPosibleShootByEnemy = false;

            for (Trooper trooper : listOfEnemys) {
                if (canSeeOrCanShoot(trooper, self, true) || canSeeOrCanShoot(trooper, self, false)) {
                    isPosibleShootByEnemy = true;
                }
            }

            if (isPosibleShootByEnemy) {

                thePoint point1 = findNotAchievableTail(self, false);
                thePoint point2 = findNotAchievableTail(self, true);
                thePoint point;

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

                    if (tempPath != null && tempPath.size() != 0 && tempPath.size() != 1 && (tempPath.size() - 1) * 2 + self.getShootCost() <= actionPoints) {

                        for (Trooper trooper : listOfEnemys) {
                            if (canShootOnTarget(self, trooper)) {
                                shootOnTarget(self, trooper);
                                return true;
                            }
                        }

                        if (self.getStance() != TrooperStance.STANDING && game.getStanceChangeCost() >= actionPoints) {
                            move.setAction(ActionType.RAISE_STANCE);
                            return true;
                        }

                        if (goOnPath(self, point.getX(), point.getY(), true)) {
                            return true;
                        }

                    } else if (tempPath != null && tempPath.size() != 0 && tempPath.size() != 1 && (tempPath.size() - 1) * 2 <= actionPoints) {

                        if (self.getStance() != TrooperStance.STANDING && game.getStanceChangeCost() >= actionPoints) {
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
                    /*if (cellsIntTemp[i][j] == -1) {
                        mapOfPoints[i][j] = 0;
                        continue;
                    }*/

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

                        /*boolean test = true;
                        for(ArrayList<thePoint> lineTemp : refMas) {
                            if (line != lineTemp && refMas.indexOf(line) / 2 == refMas.indexOf(lineTemp) / 2 && (lineTemp.size() == 3 || lineTemp.size() == 1 || lineTemp.size() == 0)) {
                                test = false;
                            }
                        }
                        if(test) {
                            mapOfPoints[i][j] = 3;
                        }

                        for(ArrayList<thePoint> line : refMas) {
                            if (line.size() == 1 && line.get(0).getX() == (line == lineUp ? i : line == lineDown ? i : line == lineLeft ? i - 1 : i + 1) && line.get(0).getY() == (line == lineUp ? j + 1 : line == lineDown ? j - 1 : line == lineLeft ? j : j)) {
                                boolean test = true;
                                for(ArrayList<thePoint> lineTemp : refMas) {
                                    if (line != lineTemp && lineTemp.size() == 3) {
                                        test = false;
                                    }
                                }
                                if(test) {
                                    mapOfPoints[i][j] = 3;
                                }
                            }
                        }*/
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

        thePoint point = findNotAchievableTail(self, canSeeOrShoot);
        LinkedList<thePoint> tempPath;

        if (point != null && target != null) {

            tempPath = lee(self, self.getX(), self.getY(), point.getX(), point.getY(), true);

            if (tempPath != null && tempPath.size() != 0 && tempPath.size() != 1 && (tempPath.size() - 1) * getCostMoveWithStance(self) + self.getShootCost() <= self.getActionPoints()) {

                if (canShootOnTarget(self, target)) {
                    shootOnTarget(self, target);
                    return true;
                } else if (goOnPath(self, point.getX(), point.getY(), true)) {
                    return true;
                }

            } else if(tempPath != null && tempPath.size() != 0 && tempPath.size() != 1 && (tempPath.size() - 1) * getCostMoveWithStance(self) <= self.getActionPoints()){

                if (goOnPath(self, point.getX(), point.getY(), true)) {
                    return true;
                }
            }
        }

        return false;
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


    private class GameUnit {
        Trooper trooper;
        int worldMove;

        GameUnit(Trooper troop) {
            trooper = troop;
            this.worldMove = world.getMoveIndex();
        }
    }

    private class thePoint {
        int x;
        int y;

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