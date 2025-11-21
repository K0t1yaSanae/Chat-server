package net.kotiyasanae.chatserver.util;

import java.util.Random;

public class Jokes {
    private static final String[] JOKES = {
            "世界上最软的四样东西：丰川祥子的小脸，丰川祥子的小嘴，丰川祥子的小脚，总决赛的niko desuwa",
            "主播能每句话后都加个desuwa吗，我看丰川祥子这么说话特别有大小姐气质",
            "在这篇短文的开头，我要先跟每一位想看Ave Mujica的观众都说一句对不起，十分抱歉，辜负了大家的期待。突然解散是因为没有处理好自己的情绪，突然解散后被网友们攻击也是我咎由自取。我之前会把破防解散的原因归结于一些不堪入目的评论。但我觉得评论环境变成那样也是我自作自受，没有处理好自己的情绪，紧接着解散也是作为乐队键盘手的失职。实在抱歉，让大家失望了。我发这篇文章不是为了为自己开脱辩解，作为键盘手我深知错了就是错了。不过我也想刚好趁着这个机会，跟大家聊聊我的问题。",
            "你怎么能直接 commit 到我的 main 分支啊？！GitHub 上不是这样！你应该先 fork 我的仓库，然后从 develop 分支 checkout 一个新的 feature 分支，比如叫 feature/confession。然后你把你的心意写成代码，并为它写好单元测试和集成测试，确保代码覆盖率达到95%以上。接着你要跑一下 Linter，通过所有的代码风格检查。然后你再 commit，commit message 要遵循 Conventional Commits 规范。之后你把这个分支 push 到你自己的远程仓库，然后给我提一个 Pull Request。在 PR 描述里，你要详细说明你的功能改动和实现思路，并且 @ 我和至少两个其他的评审。我们会 review 你的代码，可能会留下一些评论，你需要解决所有的 thread。等 CI/CD 流水线全部通过，并且拿到至少两个 LGTM 之后，我才会考虑把你的分支 squash and merge 到 develop 里，等待下一个版本发布。你怎么直接上来就想 force push 到 main？！GitHub 上根本不是这样！我拒绝合并！。",
            "从crychic到Ave Mujica，我感觉自己在进入乐队行业的这几年间，除了说话技术以外很多地方都获得了成长。但在我的性格深处却始终有那么几个可以被称为劣根的存在，始终困扰着我。这些年，我在这些地方毫无成长。比如我喜欢逃避，生活中或乐队上不顺心的事就想轻松的得过且过，先放着再说，实际上这会酿成更大的错误；比如我的讨好型人格，看到对你出言不逊恶语相加的人，我的第一反应通常不是回击他，而是试图好言相劝让对方理解自己；比如我做事三分钟热度，对组乐队最开始充满了兴趣，但是一到实际操作，我又会觉得很麻烦",
            "那一夜，丰川祥子沉思良久。她想到无数玩乐队的少女们，无数为乐队服务的staff。难道他们的流汗付出就白干了吗？她不甘心。2025年2月14日，丰川祥子的生日这天，她走出出租屋，对周围的成员说了一句令人震惊的话：当年我带你们搞乐队，现在我又要带你们搞乐队了。",
            "男人一生只吃四双袜子：长崎素世黑色小腿袜，丰川祥子的花边白袜，若叶睦的淡黄色短袜，千早爱音的羽丘绿袜",
            "千早爱音是A罩杯\uD83E\uDD70丰川祥子是超大杯\uD83E\uDD70玩机器是啥杯\uD83E\uDD2E\uD83D\uDC4E\uD83C\uDFFB",
            "@NotNull 丰川祥子 丰川祥子 = new 丰川祥子() if(keyword=='avemujica'){System.out.println(\"我已经忘记一切了\");}else System.out.println(“过去软弱的我已经死了”);",
            "世界上最软的几样东西：丰川祥子的小脸，丰川祥子的小嘴，生死局niko的脚",
            "主播能不要每句话后面都加一个desuwa吗，我侄女就这样说话，我好想她",
            "@三角初华：重组crychic是什么意思？你的乐队观念怎么了？你才16岁就搞两个乐队，再这样下去32岁搞4个，64岁搞8个，最后变成章鱼博士",
            "我可是AveMujica的Oblivionis！丰川祥子\uD83D\uDE21！要成为神明的女人\uD83D\uDE21！",
            "太有音乐了李神\uD83D\uDE00\uD83D\uDC4D太有节奏了李神\uD83D\uDE00\uD83D\uDC4D太有投技了李神\uD83D\uDE00\uD83D\uDC4D太有rap了李神\uD83D\uDE00\uD83D\uDC4D太有默认了李神\uD83D\uDE00\uD83D\uDC4D太有战术了李神\uD83D\uDE00\uD83D\uDC4D",
            "这个丰川祥子不想弹就滚去crychic行不行（不是长崎素世）",
            "丰川niko打的要急眼了转身一看若叶海参高兴地说全都狙不中"
    };

    private static final Random random = new Random();
    public static String getRandomJoke() {
        return JOKES[random.nextInt(JOKES.length)];
    }
}