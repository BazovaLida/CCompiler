.386
.model flat, stdcall
option casemap :none

include C:\masm32\include\kernel32.inc
include C:\masm32\include\user32.inc

includelib C:\masm32\lib\kernel32.lib
includelib C:\masm32\lib\user32.lib

main PROTO

.data
msg_title db "Result", 0
buffer db 128 dup(?)
format db "%d",0

.code
start:
	invoke main
	invoke wsprintf, addr buffer, addr format, eax
	invoke MessageBox, 0, addr buffer, addr msg_title, 0
	invoke ExitProcess, 0

main PROC
	push ebp
	mov ebp, esp

	push 9

	pop [ebp-4]
	push 90

	pop [ebp-8]
	push [ebp-4]     ;a_val

	push [ebp-8]     ;b_val

	mov edx, 0
	pop ECX
	pop EAX
	idiv ECX
	push EAX

	pop [ebp-12]
	push [ebp-4]     ;a_val

	push [ebp-8]     ;b_val

	mov edx, 0
	pop ECX
	pop EAX
	idiv ECX
	push EAX

	push [ebp-12]     ;c_val

	mov edx, 0
	pop ECX
	pop EAX
	idiv ECX
	push EAX

	pop [ebp-16]
	push [ebp-4]     ;a_val

	push [ebp-8]     ;b_val

	push [ebp-12]     ;c_val

	mov edx, 0
	pop ECX
	pop EAX
	idiv ECX
	push EAX

	mov edx, 0
	pop ECX
	pop EAX
	idiv ECX
	push EAX

	push [ebp-16]     ;k_val

	pop EBX
	neg EBX
	push EBX

	pop ECX
	pop EAX
	cmp eax, 0   ; check if e1 is true
	jne _clause2   ; e1 is not 0, so we need to evaluate clause 2
	jmp _end
	_clause2:
		cmp ecx, 0 ; check if e2 is true
		mov eax, 0
		setne al

	_end:
		push eax

if	push 0

	pop [ebp-20]
	push 20

	pop [ebp-24]
	push 30

	pop [ebp-28]
}
{
}
{
	push 1

	pop EBX
	neg EBX
	push EBX

	pop [ebp-4]
	push 10

	pop [ebp-24]
}
{
else	push 0

	pop [ebp-20]
}
{
	push [ebp-16]     ;k_val

	pop eax ;here is the result
	mov esp, ebp  ; restore ESP; now it points to old EBP
	pop ebp       ; restore old EBP; now ESP is where it was before prologue
	ret
main
main ENDP
END start
