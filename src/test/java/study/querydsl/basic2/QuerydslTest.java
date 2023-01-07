package study.querydsl.basic2;


import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.h2.expression.Expression;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;
import static org.assertj.core.api.Assertions.*;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

import static org.hibernate.criterion.Restrictions.allEq;
import static study.querydsl.entity.QMember.member;


@SpringBootTest
@Transactional
public class QuerydslTest {

    @PersistenceContext
    EntityManager em;

    JPAQueryFactory jpaQueryFactory; //필드로 주입!

    @BeforeEach
    public void before() {
        jpaQueryFactory = new JPAQueryFactory(em);

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);


        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    public void Testcall() {
        List<String> result = jpaQueryFactory
                .select(member.username)
                .from(member)
                .fetch();

        for(String s : result) {
            System.out.println("s = " + s);
        }
    }
    @Test
    public void tupleProjection() {
        List<Tuple> result = jpaQueryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for(Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username = " + username);
            System.out.println("age = " + age);
        }
    }
    @Test
    public void findDtoByJPQL() { // 권장x

        List<MemberDto> result = em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
                .getResultList();

        for(MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }
    @Test
    public void findDtoBySetter() { //세터를 사용한 프로퍼티 접근 방법
        List<MemberDto> result = jpaQueryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findDtoByField() { //필드를 사용한 프로퍼티 접근 방법
        List<MemberDto> result = jpaQueryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findDtoByConstructor() { //생성자를 사용한 프로퍼티 접근 방법
        List<MemberDto> result = jpaQueryFactory
                .select(Projections.constructor(MemberDto.class,
                        member.username,  //사용할 때 타입이 맞아야함
                        member.age))  //사용할 때 타입이 맞아야함
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }
    @Test
    public void findDtoByQueryProjection() { //querydsl의 의존도를 높게 가져가고싶을경우 사용
        // dto를 깔끔하게 가져가고싶다면 위 3가지 방식을 사용함
        List<MemberDto> result = jpaQueryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        for(MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto );
        }
    }
    @Test
    public void dynamicQuery_BooleanBuilder() {
        String usernameParam = "member1";
        Integer ageParam = null;

        List<Member> result = searchMember1(usernameParam,ageParam);
        assertThat(result.size()).isEqualTo(1);

    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {

        BooleanBuilder builder = new BooleanBuilder();
        if (usernameCond != null) {  //유저네임컨디션이 null이 아니면 멤버의 유저네임이 같다
            builder.and(member.username.eq(usernameCond));
        }

        if (ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }

        return jpaQueryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    @Test
    public void dynamicQuery_WhereParam() {  //권장사용방식 //메서드가 빠져서 조립이 가능함
        String usernameParam = "member1";
        Integer ageParam = null;

        List<Member> result = searchMember2(usernameParam,ageParam);
        assertThat(result.size()).isEqualTo(1);

    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return jpaQueryFactory
                .selectFrom(member)
                .where(usernameEq(usernameCond),ageEq(ageCond))
//                .where(allEq(usernameCond,ageCond))
                .fetch();
    }

    private BooleanExpression usernameEq(String usernameCond) {
        return usernameCond != null ? member.username.eq(usernameCond) : null;
//        usernameCond이 null이 아니면 해당값을 반환                               / 아닐경우 null 반환
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null;
//        ageCond이 null이 아니면 해당값을 반환                     / 아닐경우 null 반환
    }
//    private BooleanExpression allEq(String usernameCond, Integer ageCond) {
//        return usernameEq(usernameCond).and(ageEq(ageCond));
//    }

    /**
     * 벌크연산 다량의 정보를 한번엥 업데이트하거나 변경하는 것
     * 주의점: 영속성컨텍스트의 정보와 DB의 상태가 일치하지않아짐 두개의 상태가 일치하지않으면 영속성컨텍스트의 정보가 남게됨
     * em.flush();/em.clear(); 메서드를 통해 이의 경우 초기화해야함
     */
    @Test
    @Commit
    public void bulkUpdate() {

        //member1 = 10 ->비회원
        //member1 = 20 ->비회원
        //member1 = 30 ->유지
        //member1 = 40 ->유지



        jpaQueryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();

        em.flush();
        em.clear();

        List<Member> result = jpaQueryFactory
                .selectFrom(member)
                .fetch();

        for(Member member1 : result) {
            System.out.println("member1 = " + member1);
        }
    }

    /**
     * 모든 회원 1살 추가
     */
    @Test
    public void bulkAdd() {
        jpaQueryFactory
                .update(member)
                .set(member.age, member.age.add(1))
                .execute();
    }
    /**
     * 모든 회원 1살 추가
     */
    @Test
    public void bulkMultiply() {
        jpaQueryFactory
                .update(member)
                .set(member.age, member.age.multiply(2))
                .execute();
    }
    /**
     * 삭제
     */
    @Test
    public void bulkDelete() {
        jpaQueryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();
    }

    /**
     * SQL function
     */
    @Test
    public void sqlFunction() {
        List<String> result = jpaQueryFactory
                .select(Expressions.stringTemplate(
                        "function('replace', {0}, {1}, {2})",
                        member.username, "member", "M"))
                .from(member)
                .fetch();

        for(String s : result) {
            System.out.println("s = " + s);
        }
    }

    /**
     * 소문자로 바꾸기
     */
    @Test
    public void sqlFunction2() {
        List<String> result = jpaQueryFactory
                .select(member.username)
                .from(member)
//                 .where(member.username.eq(Expressions.stringTemplate("function('lower', {0})", member.username)))  방법1
                .where(member.username.eq(member.username.lower()))  //방법2 이 방법이 더 깔끔함
                .fetch();
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }
}
