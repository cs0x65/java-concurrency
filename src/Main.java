import main.Snippet;

import java.util.Arrays;

public class Main {
    public static void main(String[] args){
        if(args.length > 0){
            try {
                Class<? extends Snippet> clazz = (Class<? extends Snippet>)Class.forName(args[0]);
                Snippet snippet =  clazz.newInstance();

                String[] snippetArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : null;
                snippet.runSnippet(snippetArgs);
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
            return;
        }
        System.out.println("Error: missing test code snippet to invoke! \nPlease pass a fully qualified classname " +
                "of the code snippet to test; such class shall implement interface: main.TestSnippet");
    }
}
