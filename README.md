RussianAICup2013
================

!Update! 53 place in rating.

CodeTroopers 2013

Всё было очень быстро, так как хоть и проводился чемпионат месяц, к сожалению времени было только вечером и в выходные. Так же предыдущие версии были выше, но новые идеи и интерес требовали идти вперёд не смотря на место.

К сожалению, про тестирование подумал поздно, хотя львиную долю времени как раз убил на поиск и исправление ошибок в коде после обновлений.

Реализовано:

-Обновление глобальных целей по углам, в начале глобальная точка в центре, использование локальный целей (последний видимый противник).

-Использование абилки командора.

-Командир и разведчик идут первыми, за ними солдат, снайпер и медик.

-При отсутствии перестрелки во время хода, используются ограничения по радиусу от некоторых юнитов.

-Поиск кратчайшего пути (волновой алгоритм, lee).

-Медик привязан изначально к снайперу.

-Атака и отбегание из видимости юнита или из радиуса досягаемости, если нет возможности уйти из радиуса видимости.

-Стрельба по ранее видимым целям.

-Использование гранат (в данной версии есть баг, ещё не пофиксен, вместо выбора лучшей точки из возможных выбирает
только из 2-х), нахождение лучшей точки с максимальным уроном и проверка имеет ли смысл бросать гранату.

-Передвижение с использованием карты опасности, использование по возможности безопасных мест.

-Реагирование на падение стратегии оппонента.

-Хиляние медиком

-Уход из поля зрения врага при значительном повреждении.

-Обранужение атаки из "ниоткуда", единственное что не успел добавить расчёт примерной позиции противника. После такой атаки юниты просто игнорируют её и бегут дальше.

-Подбор бонусов в мирное время

-Использование бонусов: лечение, расчёт броска гранаты с учётом FIELD_RATION и т. п.

-Понижение и повышение положения юнита для лучшей стрельбы. Для скрытия от врага сделать не успел, хотя вещь очень и очень полезная.

-Обход своих стоящих юнитов

-Учёт действий юнитов от кол-ва юнитов соперника при атаке. Тоже ещё много идей, широкое поле для улучшения.

-Выбор целей для атаки исходя из дистанции, статуса юнита (разведчик, командир, снайпер и т. д.), его здоровья.

Многое чего хотел переделать, что-то переписал заново, что-то так и не решил делать из-за лимита времени, например переписать код отвечающий за ходилку в нормальный вид. Тест шёл в ручную, начальный в локалранере, дальнейшее после заливки на сервер.
