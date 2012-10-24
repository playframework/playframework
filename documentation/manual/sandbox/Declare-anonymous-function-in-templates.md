Sometimes you need to declare a variable in your template, but since we're more in a functional environnement we will prefer to declare an anonymous function thanks to `@using(){}` :

    <ul>
        @using( Map(...) ){ values => 
            @for( toto <- values ){
                <li>toto</li>
            }
        }
    </ul>