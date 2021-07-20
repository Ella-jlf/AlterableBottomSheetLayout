для работы необходимы анимации

    implementation "androidx.dynamicanimation:dynamicanimation:1.0.0"

foreground - ОБЯЗАТЕЛЬНО!!! требуется ссылка на bg_some_background.xml(нужно для указания закругленных углов)
head_layout - дефолтный layout(frame=default,linear,relative). Если требуется иной, можно просто вложить в этот bottom sheet любой layout через xml. 
transparency_percent - от 0.0 до 1.0 процент прозрачности
background_color - цвет заднего фона
foreground_color - цвет переднего фона(или указать сразу при передаче в foreground)
margin_top - отступ сверху
scrollable_area_span - ширина области, по нажатию на которую будет доступен скрол bottom sheet-а, считается от верхней точки foreground и вниз на указанное dp.
hide_on_background_click - скрывать по нажатию на задний фон
isHidable - определяет можно ли вообще её скрыть scroll-ом или по нажатию на background(если доступно), при false требуется скрывать ручками через метод hide()
_____________________________________________
show() - показывает bottom sheet
hide() - скрывает его