package study.querydsl.basic;

import com.querydsl.core.QueryResults;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import java.util.List;

import static com.querydsl.jpa.JPAExpressions.select;
import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.*;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

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
    public void startQuerydsl() {
        QMember m = new QMember("m");

        Member findMember = jpaQueryFactory
                .select(m)
                .from(m)
                .where(m.username.eq("member1"))
                .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test //기본 Q타입 문법
    public void startQuerydsl2() {
        Member findMember = jpaQueryFactory
                .select(QMember.member)
                .from(QMember.member)
                .where(member.username.eq("member1"))
                .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");

    }



    @Test //검색 조건 쿼리 and 사용하기
    public void search() {
        Member findMember = jpaQueryFactory
                .selectFrom(member)
                .where(member.username.eq("member1").and(member.age.eq(10)))
                .fetchOne(); //단 건 조회
        assertThat(findMember.getUsername()).isEqualTo("member1");
        assertThat(findMember.getAge()).isEqualTo(10);
    }

    @Test //검색 조건 쿼리  //and없이 ,사용도 가능 / 더 깔끔하고 권장되는 방식 *null을 무시하므로
    public void searchAndParam() {
        Member findMember = jpaQueryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"),
                        member.age.eq(10)
                )
                .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");
        assertThat(findMember.getAge()).isEqualTo(10);
    }
    @Test
    public void resultFetch() {
        List<Member> fetch = jpaQueryFactory  //option + enter
                .selectFrom(member)
                .fetch();  //list로 조회

        Member fetchOne = jpaQueryFactory
                .selectFrom(member)
                .fetchOne(); //단 건 조회

        Member fetchFirst = jpaQueryFactory
                .selectFrom(member)
                .fetchFirst();  // 처음 단 건 조회

        QueryResults<Member> Results = jpaQueryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();  //전체조회건수/페이징에서 사용
        assertThat(Results.getTotal()).isEqualTo(4);
        assertThat(Results.getLimit()).isEqualTo(2);
        assertThat(Results.getOffset()).isEqualTo(1);
        assertThat(Results.getResults().size()).isEqualTo(2);

        long count = jpaQueryFactory
                .selectFrom(member)
                .fetchCount();
    }
    /** 정렬
     * 회원 정렬 순서
     * 1. 회원 나이 내립차순(desc)
     * 2. 회원 이름 올림차순(asc)
     * 단 2에서 회원 이름이없으면 마지막에 출력(nulls last)
     */
    @Test
    public void sort() {

        em.persist(new Member("member5",100));
        em.persist(new Member("member6",100));
        em.persist(new Member(null,100));

        List<Member> result = jpaQueryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(),member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);
        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }
    @Test
    public void paging1() {
        jpaQueryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();
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
}
















